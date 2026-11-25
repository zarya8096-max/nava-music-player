package com.nava.music.playback

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.nava.music.data.AiConfig
import com.nava.music.data.AiLyricsRepository
import com.nava.music.data.AppThemeMode
import com.nava.music.data.AudioEnergy
import com.nava.music.data.LibraryFilter
import com.nava.music.data.LyricsPayload
import com.nava.music.data.MediaStoreRepository
import com.nava.music.data.PaletteSeed
import com.nava.music.data.PlayerViewMode
import com.nava.music.data.RepeatMode
import com.nava.music.data.SettingsRepository
import com.nava.music.data.Track
import com.nava.music.data.VinylAtmosphere
import com.nava.music.data.VisualizerStyle
import com.nava.music.ui.theme.DynamicThemeEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LyricsUiState(
    val isLoading: Boolean = false,
    val isTranslating: Boolean = false,
    val payload: LyricsPayload? = null,
    val errorMessage: String? = null,
    val showTranslation: Boolean = true
)

data class PlayerUiState(
    val tracks: List<Track> = emptyList(),
    val queue: List<Track> = emptyList(),
    val currentTrack: Track? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val favorites: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val activeFilter: LibraryFilter = LibraryFilter.ALL,
    val paletteSeed: PaletteSeed = DynamicThemeEngine.defaultSeed,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.HALO_AND_WAVE,
    val playerViewMode: PlayerViewMode = PlayerViewMode.VINYL,
    val vinylAtmosphere: VinylAtmosphere = VinylAtmosphere.AURORA,
    val aiConfig: AiConfig = AiConfig(),
    val lyricsState: LyricsUiState = LyricsUiState(),
    val hasAudioPermission: Boolean = false,
    val hasRecordPermission: Boolean = false,
    val isLoadingLibrary: Boolean = false
)

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaStoreRepository = MediaStoreRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val aiLyricsRepository = AiLyricsRepository(settingsRepository)
    private val visualizerEngine = BassVisualizerEngine(viewModelScope)

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            favorites = settingsRepository.loadFavorites(),
            themeMode = settingsRepository.loadThemeMode(),
            visualizerStyle = settingsRepository.loadVisualizerStyle(),
            playerViewMode = settingsRepository.loadPlayerViewMode(),
            vinylAtmosphere = settingsRepository.loadVinylAtmosphere(),
            aiConfig = settingsRepository.loadAiConfig()
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    val audioEnergy: StateFlow<AudioEnergy> = visualizerEngine.energy

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionTickerJob: Job? = null
    private var paletteJob: Job? = null
    private var lyricsJob: Job? = null

    init {
        connectMediaController()
        observeAudioSession()
    }

    private fun connectMediaController() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, NavaPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    mediaController = controller
                    controller.addListener(playerListener)
                    syncFromController(controller)
                    startPositionTicker()
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun observeAudioSession() {
        viewModelScope.launch {
            NavaPlaybackService.audioSessionId.collect { sessionId ->
                val state = _uiState.value
                visualizerEngine.attach(
                    sessionId = sessionId,
                    hasRecordPermission = state.hasRecordPermission,
                    isPlaying = state.isPlaying
                )
            }
        }
    }

    fun setVisualizerUiActive(active: Boolean) {
        val state = _uiState.value
        visualizerEngine.setUiActive(
            active = active,
            sessionId = NavaPlaybackService.audioSessionId.value,
            hasRecordPermission = state.hasRecordPermission,
            isPlaying = state.isPlaying
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            visualizerEngine.attach(
                sessionId = NavaPlaybackService.audioSessionId.value,
                hasRecordPermission = _uiState.value.hasRecordPermission,
                isPlaying = isPlaying
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaController?.let { syncFromController(it) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            mediaController?.let { syncFromController(it) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mapped = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
            _uiState.update { it.copy(repeatMode = mapped) }
        }
    }

    private fun syncFromController(controller: MediaController) {
        val queue = _uiState.value.queue
        val idx = controller.currentMediaItemIndex
        val matchedTrack = queue.getOrNull(idx)
        val prevTrackId = _uiState.value.currentTrack?.id

        _uiState.update {
            it.copy(
                currentTrack = matchedTrack ?: it.currentTrack,
                currentIndex = idx,
                isPlaying = controller.isPlaying,
                durationMs = controller.duration.takeIf { d -> d > 0 }
                    ?: matchedTrack?.durationMs
                    ?: it.durationMs,
                positionMs = 0L
            )
        }

        if (matchedTrack != null && matchedTrack.id != prevTrackId) {
            updateDynamicSeed(matchedTrack)
            val cached = settingsRepository.loadCachedLyrics(matchedTrack.cacheKey)
            _uiState.update {
                it.copy(
                    lyricsState = LyricsUiState(
                        payload = cached,
                        showTranslation = true
                    )
                )
            }
        }
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    _playbackPositionMs.value = pos
                    val dur = controller.duration
                    if (dur > 0 && dur != _uiState.value.durationMs) {
                        _uiState.update { it.copy(durationMs = dur) }
                    }
                }
                delay(250L)
            }
        }
    }

    fun onPermissionsUpdated(hasAudio: Boolean, hasRecord: Boolean) {
        _uiState.update {
            it.copy(hasAudioPermission = hasAudio, hasRecordPermission = hasRecord)
        }
        if (hasAudio && _uiState.value.tracks.isEmpty()) {
            refreshLibrary()
        }
        visualizerEngine.attach(
            sessionId = NavaPlaybackService.audioSessionId.value,
            hasRecordPermission = hasRecord,
            isPlaying = _uiState.value.isPlaying
        )
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLibrary = true) }
            val loaded = mediaStoreRepository.loadLocalTracks()
            _uiState.update { state ->
                state.copy(
                    tracks = loaded,
                    queue = state.queue.ifEmpty { loaded },
                    isLoadingLibrary = false
                )
            }
        }
    }

    fun playTrack(track: Track, fromList: List<Track> = _uiState.value.tracks) {
        val controller = mediaController ?: return
        val targetQueue = if (fromList.isNotEmpty()) fromList else listOf(track)
        val index = targetQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        val mediaItems = targetQueue.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id.toString())
                .setUri(item.contentUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist(item.artist)
                        .setAlbumTitle(item.album)
                        .setArtworkUri(item.albumArtUri)
                        .build()
                )
                .build()
        }

        _playbackPositionMs.value = 0L
        _uiState.update {
            it.copy(
                queue = targetQueue,
                currentTrack = track,
                currentIndex = index,
                durationMs = track.durationMs,
                positionMs = 0L
            )
        }
        updateDynamicSeed(track)

        val cached = settingsRepository.loadCachedLyrics(track.cacheKey)
        _uiState.update { it.copy(lyricsState = LyricsUiState(payload = cached)) }

        controller.setMediaItems(mediaItems, index, 0L)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) {
            val first = _uiState.value.tracks.firstOrNull() ?: return
            playTrack(first)
            return
        }
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun skipNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (controller.mediaItemCount > 0) {
            controller.seekTo(0, 0L)
        }
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3500L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        mediaController?.seekTo(target)
        _playbackPositionMs.value = target
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val next = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = next
        _uiState.update { it.copy(shuffleEnabled = next) }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        controller.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        _uiState.update { it.copy(repeatMode = next) }
    }

    fun toggleFavorite(trackId: Long) {
        val updated = _uiState.value.favorites.toMutableSet()
        if (!updated.add(trackId)) {
            updated.remove(trackId)
        }
        settingsRepository.saveFavorites(updated)
        _uiState.update { it.copy(favorites = updated) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        settingsRepository.saveThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun cycleThemeMode() {
        val all = AppThemeMode.entries
        val next = all[(all.indexOf(_uiState.value.themeMode) + 1) % all.size]
        setThemeMode(next)
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        settingsRepository.saveVisualizerStyle(style)
        _uiState.update { it.copy(visualizerStyle = style) }
    }

    fun setPlayerViewMode(mode: PlayerViewMode) {
        settingsRepository.savePlayerViewMode(mode)
        _uiState.update { it.copy(playerViewMode = mode) }
    }

    fun setVinylAtmosphere(atmosphere: VinylAtmosphere) {
        settingsRepository.saveVinylAtmosphere(atmosphere)
        _uiState.update { it.copy(vinylAtmosphere = atmosphere) }
    }

    fun saveAiConfig(config: AiConfig) {
        settingsRepository.saveAiConfig(config)
        _uiState.update { it.copy(aiConfig = config) }
    }

    fun fetchLyricsForCurrentTrack(forceRefresh: Boolean = false) {
        val track = _uiState.value.currentTrack ?: return
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _uiState.update {
                it.copy(lyricsState = it.lyricsState.copy(isLoading = true, errorMessage = null))
            }
            val result = aiLyricsRepository.fetchLyrics(
                track = track,
                aiConfig = _uiState.value.aiConfig,
                forceRefresh = forceRefresh
            )
            result.fold(
                onSuccess = { payload ->
                    _uiState.update {
                        it.copy(
                            lyricsState = it.lyricsState.copy(
                                isLoading = false,
                                payload = payload,
                                errorMessage = null
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            lyricsState = it.lyricsState.copy(
                                isLoading = false,
                                errorMessage = err.message ?: "خطا در دریافت لیریک"
                            )
                        )
                    }
                }
            )
        }
    }

    fun translateCurrentLyrics() {
        val track = _uiState.value.currentTrack ?: return
        val existing = _uiState.value.lyricsState.payload ?: return
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _uiState.update {
                it.copy(lyricsState = it.lyricsState.copy(isTranslating = true, errorMessage = null))
            }
            val result = aiLyricsRepository.translateLyrics(
                track = track,
                existing = existing,
                aiConfig = _uiState.value.aiConfig
            )
            result.fold(
                onSuccess = { translated ->
                    _uiState.update {
                        it.copy(
                            lyricsState = it.lyricsState.copy(
                                isTranslating = false,
                                payload = translated,
                                showTranslation = true,
                                errorMessage = null
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            lyricsState = it.lyricsState.copy(
                                isTranslating = false,
                                errorMessage = err.message ?: "خطا در ترجمه لیریک"
                            )
                        )
                    }
                }
            )
        }
    }

    fun toggleShowTranslation() {
        _uiState.update {
            it.copy(lyricsState = it.lyricsState.copy(showTranslation = !it.lyricsState.showTranslation))
        }
    }

    private fun updateDynamicSeed(track: Track) {
        paletteJob?.cancel()
        paletteJob = viewModelScope.launch {
            val extracted = DynamicThemeEngine.extractSeedFromArtwork(
                context = getApplication(),
                contentUri = track.contentUri,
                artUri = track.albumArtUri,
                fallbackSeed = "${track.artist}-${track.title}"
            )
            _uiState.update { it.copy(paletteSeed = extracted) }
        }
    }

    override fun onCleared() {
        positionTickerJob?.cancel()
        visualizerEngine.release()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}

package com.nava.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nava.music.data.PlayerViewMode
import com.nava.music.data.VisualizerStyle
import com.nava.music.playback.MusicPlayerViewModel
import com.nava.music.ui.screens.LibraryScreen
import com.nava.music.ui.screens.LyricsSheet
import com.nava.music.ui.screens.NowPlayingSheet
import com.nava.music.ui.screens.QueueSheet
import com.nava.music.ui.screens.SettingsSheet
import com.nava.music.ui.screens.VinylPlayerView
import com.nava.music.ui.theme.NavaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            NavaApp()
        }
    }
}

@Composable
private fun NavaApp(
    viewModel: MusicPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val energy by viewModel.audioEnergy.collectAsState()

    var showNowPlaying by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun checkPermissions() {
        val hasAudio = ContextCompat.checkSelfPermission(context, audioPermission) ==
            PackageManager.PERMISSION_GRANTED
        val hasRecord = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionsUpdated(hasAudio = hasAudio, hasRecord = hasRecord)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkPermissions()
    }

    LaunchedEffect(Unit) {
        checkPermissions()
        val toRequest = buildList {
            if (ContextCompat.checkSelfPermission(context, audioPermission) != PackageManager.PERMISSION_GRANTED) {
                add(audioPermission)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    BackHandler(enabled = showSettings || showQueue || showLyrics || showNowPlaying) {
        when {
            showSettings -> showSettings = false
            showQueue -> showQueue = false
            showLyrics -> showLyrics = false
            showNowPlaying -> showNowPlaying = false
        }
    }

    NavaTheme(palette = state.palette) {
        Box(modifier = Modifier.fillMaxSize()) {
            LibraryScreen(
                state = state,
                energy = energy,
                onRequestAudioPermission = {
                    permissionLauncher.launch(arrayOf(audioPermission, Manifest.permission.RECORD_AUDIO))
                },
                onRequestRecordPermission = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                },
                onRefresh = { viewModel.refreshLibrary() },
                onSearchChange = { viewModel.updateSearchQuery(it) },
                onFilterSelect = { viewModel.selectFilter(it) },
                onTrackClick = { track, list ->
                    viewModel.playTrack(track, list)
                },
                onShuffleAll = {
                    val shuffled = state.tracks.shuffled()
                    shuffled.firstOrNull()?.let { first ->
                        viewModel.playTrack(first, shuffled)
                        showNowPlaying = true
                    }
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSkipNext = { viewModel.skipNext() },
                onOpenNowPlaying = { showNowPlaying = true },
                onOpenSettings = { showSettings = true }
            )

            // Full-Screen Now Playing Overlay (Modern <-> Hi-Fi Vinyl Turntable Crossfade)
            AnimatedVisibility(
                visible = showNowPlaying && state.currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Crossfade(
                    targetState = state.playerViewMode,
                    animationSpec = tween(420),
                    label = "playerModeCrossfade"
                ) { mode ->
                    when (mode) {
                        PlayerViewMode.VINYL -> {
                            VinylPlayerView(
                                state = state,
                                energy = energy,
                                onCollapse = { showNowPlaying = false },
                                onSwitchToModern = { viewModel.setPlayerViewMode(PlayerViewMode.MODERN) },
                                onSelectAtmosphere = { viewModel.setVinylAtmosphere(it) },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onSkipPrevious = { viewModel.skipPrevious() },
                                onSeekTo = { viewModel.seekTo(it) },
                                onToggleShuffle = { viewModel.toggleShuffle() },
                                onCycleRepeat = { viewModel.cycleRepeatMode() },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onOpenLyrics = { showLyrics = true },
                                onOpenQueue = { showQueue = true }
                            )
                        }

                        PlayerViewMode.MODERN -> {
                            NowPlayingSheet(
                                state = state,
                                energy = energy,
                                onCollapse = { showNowPlaying = false },
                                onSwitchToVinyl = { viewModel.setPlayerViewMode(PlayerViewMode.VINYL) },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onSkipPrevious = { viewModel.skipPrevious() },
                                onSeekTo = { viewModel.seekTo(it) },
                                onToggleShuffle = { viewModel.toggleShuffle() },
                                onCycleRepeat = { viewModel.cycleRepeatMode() },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onCycleVisualizerStyle = {
                                    val all = VisualizerStyle.entries
                                    val next = all[(all.indexOf(state.visualizerStyle) + 1) % all.size]
                                    viewModel.setVisualizerStyle(next)
                                },
                                onOpenLyrics = { showLyrics = true },
                                onOpenQueue = { showQueue = true },
                                onOpenSettings = { showSettings = true }
                            )
                        }
                    }
                }
            }

            // AI Lyrics & Translation Sheet
            AnimatedVisibility(
                visible = showLyrics && state.currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                LyricsSheet(
                    state = state,
                    onClose = { showLyrics = false },
                    onFetchLyrics = { force -> viewModel.fetchLyricsForCurrentTrack(force) },
                    onTranslateLyrics = { viewModel.translateCurrentLyrics() },
                    onToggleTranslation = { viewModel.toggleShowTranslation() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onOpenSettings = { showSettings = true }
                )
            }

            // Queue Sheet
            AnimatedVisibility(
                visible = showQueue,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                QueueSheet(
                    state = state,
                    onSelectTrack = { track ->
                        viewModel.playTrack(track, state.queue)
                        showQueue = false
                    },
                    onClose = { showQueue = false }
                )
            }

            // AI & Visualizer Settings Sheet
            AnimatedVisibility(
                visible = showSettings,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SettingsSheet(
                    initialConfig = state.aiConfig,
                    visualizerStyle = state.visualizerStyle,
                    hasRecordPermission = state.hasRecordPermission,
                    onSelectVisualizerStyle = { viewModel.setVisualizerStyle(it) },
                    onRequestRecordPermission = {
                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    },
                    onSaveConfig = { viewModel.saveAiConfig(it) },
                    onClose = { showSettings = false }
                )
            }
        }
    }
}

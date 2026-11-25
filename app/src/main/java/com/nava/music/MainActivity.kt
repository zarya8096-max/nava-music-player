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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nava.music.data.AppThemeMode
import com.nava.music.data.PlayerViewMode
import com.nava.music.data.VisualizerStyle
import com.nava.music.playback.MusicPlayerViewModel
import com.nava.music.ui.screens.LibraryScreen
import com.nava.music.ui.screens.LyricsSheet
import com.nava.music.ui.screens.NowPlayingSheet
import com.nava.music.ui.screens.QueueSheet
import com.nava.music.ui.screens.SettingsSheet
import com.nava.music.ui.screens.VinylPlayerView
import com.nava.music.ui.theme.DynamicThemeEngine
import com.nava.music.ui.theme.NavaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NavaApp(
                onSystemBarsThemeChanged = { isDark ->
                    val style = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
            )
        }
    }
}

@Composable
private fun NavaApp(
    onSystemBarsThemeChanged: (Boolean) -> Unit,
    viewModel: MusicPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()

    val systemDark = isSystemInDarkTheme()
    val isDark = when (state.themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    LaunchedEffect(isDark) {
        onSystemBarsThemeChanged(isDark)
    }

    val resolvedPalette = remember(state.paletteSeed, isDark) {
        DynamicThemeEngine.resolvePalette(state.paletteSeed, isDark)
    }

    var showNowPlaying by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isAppForeground by remember { mutableStateOf(true) }

    // Automatically suspend FFT visualizer when app goes to background or NowPlaying closes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isAppForeground = true
                Lifecycle.Event.ON_PAUSE -> isAppForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isAppForeground, showNowPlaying, showLyrics, showQueue, showSettings) {
        val vizVisible = isAppForeground && showNowPlaying && !showLyrics && !showQueue && !showSettings
        viewModel.setVisualizerUiActive(vizVisible)
    }

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

    NavaTheme(palette = resolvedPalette) {
        Box(modifier = Modifier.fillMaxSize()) {
            LibraryScreen(
                state = state,
                positionFlow = viewModel.playbackPositionMs,
                onRequestAudioPermission = {
                    permissionLauncher.launch(arrayOf(audioPermission, Manifest.permission.RECORD_AUDIO))
                },
                onRefresh = { viewModel.refreshLibrary() },
                onSearchChange = { viewModel.updateSearchQuery(it) },
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
                onPlayAll = {
                    state.tracks.firstOrNull()?.let { first ->
                        viewModel.playTrack(first, state.tracks)
                        showNowPlaying = true
                    }
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSkipNext = { viewModel.skipNext() },
                onOpenNowPlaying = { showNowPlaying = true },
                onOpenSettings = { showSettings = true }
            )

            AnimatedVisibility(
                visible = showNowPlaying && state.currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Crossfade(
                    targetState = state.playerViewMode,
                    animationSpec = tween(360),
                    label = "playerModeCrossfade"
                ) { mode ->
                    when (mode) {
                        PlayerViewMode.VINYL -> {
                            VinylPlayerView(
                                state = state,
                                positionFlow = viewModel.playbackPositionMs,
                                energyFlow = viewModel.audioEnergy,
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
                                positionFlow = viewModel.playbackPositionMs,
                                energyFlow = viewModel.audioEnergy,
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

            AnimatedVisibility(
                visible = showLyrics && state.currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                LyricsSheet(
                    state = state,
                    positionFlow = viewModel.playbackPositionMs,
                    onClose = { showLyrics = false },
                    onFetchLyrics = { force -> viewModel.fetchLyricsForCurrentTrack(force) },
                    onTranslateLyrics = { viewModel.translateCurrentLyrics() },
                    onToggleTranslation = { viewModel.toggleShowTranslation() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onOpenSettings = { showSettings = true }
                )
            }

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

            AnimatedVisibility(
                visible = showSettings,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SettingsSheet(
                    initialConfig = state.aiConfig,
                    themeMode = state.themeMode,
                    visualizerStyle = state.visualizerStyle,
                    hasRecordPermission = state.hasRecordPermission,
                    onSelectThemeMode = { viewModel.setThemeMode(it) },
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

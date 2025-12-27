package com.rcmiku.music.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.rcmiku.music.ui.icons.ChevronDown
import com.rcmiku.music.ui.icons.Down
import com.rcmiku.music.ui.icons.Pause
import com.rcmiku.music.ui.icons.PlayArrow
import com.rcmiku.music.viewModel.MvPlayerViewModel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MvPlayerScreen(
    navController: NavHostController,
    mvPlayerViewModel: MvPlayerViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val url by mvPlayerViewModel.playUrl.collectAsState()

    var fullscreen by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    LaunchedEffect(url) {
        val u = url
        if (!u.isNullOrBlank()) {
            val mediaItem = MediaItem.fromUri(Uri.parse(u))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        }
    }

    var durationMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(player) {
        while (true) {
            durationMs = player.duration.coerceAtLeast(0L)
            positionMs = player.currentPosition.coerceAtLeast(0L)
            delay(500)
        }
    }

    fun formatMs(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    val sharedKey = mvPlayerViewModel.sharedKey

    with(sharedTransitionScope) {
        Column {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 8 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 8 })
            ) {
                Column {
                TopAppBar(
                    title = {
                        Text(
                            text = mvPlayerViewModel.title,
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                Color.Transparent
                            )
                        )
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (mvPlayerViewModel.coverUrl.isNotBlank() && sharedKey.isNotBlank()) {
                        AsyncImage(
                            model = mvPlayerViewModel.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .sharedElement(
                                    state = sharedTransitionScope.rememberSharedContentState(
                                        key = "mv_cover_" + sharedKey
                                    ),
                                    animatedVisibilityScope = animatedContentScope,
                                    boundsTransform = AlbumArtBoundsTransform,
                                )
                        )
                    }
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                this.player = player
                            }
                        },
                        update = { view ->
                            view.player = player
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (player.isPlaying) {
                                player.pause()
                            } else {
                                player.play()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (player.isPlaying) Pause else PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    val sliderPos = if (durationMs > 0L) {
                        positionMs.toFloat() / durationMs.toFloat()
                    } else {
                        0f
                    }

                    Slider(
                        value = sliderPos,
                        onValueChange = { v ->
                            if (durationMs > 0L) {
                                player.seekTo((durationMs * v).toLong())
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${formatMs(positionMs)}/${formatMs(durationMs)}",
                        style = MaterialTheme.typography.labelMedium
                    )

                    IconButton(
                        onClick = {
                            fullscreen = !fullscreen
                            activity?.requestedOrientation = if (fullscreen) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (fullscreen) ChevronDown else Down,
                            contentDescription = null
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (mvPlayerViewModel.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = mvPlayerViewModel.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mvPlayerViewModel.title,
                            maxLines = 1,
                            modifier = if (sharedKey.isNotBlank()) {
                                Modifier.sharedElement(
                                    state = sharedTransitionScope.rememberSharedContentState(
                                        key = "mv_title_" + sharedKey
                                    ),
                                    animatedVisibilityScope = animatedContentScope,
                                    boundsTransform = AlbumArtBoundsTransform,
                                )
                            } else {
                                Modifier
                            }
                        )
                        Text(
                            text = mvPlayerViewModel.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

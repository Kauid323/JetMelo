package com.rcmiku.music.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaMetadata
import coil3.compose.AsyncImage
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.constants.MiniPlayerHeight
import com.rcmiku.music.constants.ThumbnailCornerRadius
import com.rcmiku.music.ui.icons.Pause
import com.rcmiku.music.ui.icons.PlayArrow
import com.rcmiku.music.ui.icons.SkipNext

@Composable
fun MiniPlayer(
    mediaMetadata: MediaMetadata,
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current

    var dragSumX by remember { mutableStateOf(0f) }
    val swipeThresholdPx = 96f

    val showMiniPlayer =
        (playerState?.player?.mediaItemCount ?: 0) != 0

    if (showMiniPlayer)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .height(MiniPlayerHeight)
        ) {
            LinearProgressIndicator(
                progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 6.dp),
            ) {
                val player = playerState?.player
                val currentIndex = player?.currentMediaItemIndex ?: 0
                val prevMeta = player?.takeIf { it.mediaItemCount > 0 && currentIndex - 1 >= 0 }
                    ?.getMediaItemAt(currentIndex - 1)
                    ?.mediaMetadata
                val nextMeta = player?.takeIf { it.mediaItemCount > 0 && currentIndex + 1 < it.mediaItemCount }
                    ?.getMediaItemAt(currentIndex + 1)
                    ?.mediaMetadata

                Box(
                    Modifier
                        .weight(1f)
                        .pointerInput(mediaMetadata) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount ->
                                    dragSumX += dragAmount
                                },
                                onDragEnd = {
                                    val shouldNext = (dragSumX < 0f && -dragSumX >= swipeThresholdPx) && nextMeta != null
                                    val shouldPrev = (dragSumX > 0f && dragSumX >= swipeThresholdPx) && prevMeta != null
                                    when {
                                        shouldNext -> mediaController?.seekToNext()
                                        shouldPrev -> mediaController?.seekToPrevious()
                                    }
                                    dragSumX = 0f
                                },
                                onDragCancel = {
                                    dragSumX = 0f
                                }
                            )
                        }
                ) {
                    // preview layer: show next/prev info sliding in from side
                    val previewMeta = when {
                        dragSumX < 0f -> nextMeta
                        dragSumX > 0f -> prevMeta
                        else -> null
                    }
                    if (previewMeta != null && dragSumX != 0f) {
                        val previewOffset = dragSumX + if (dragSumX < 0f) 1000f else -1000f
                        MiniMediaInfo(
                            mediaMetadata = previewMeta,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .graphicsLayer { translationX = previewOffset },
                            imageModifier = imageModifier
                        )
                    }

                    // current layer follows finger
                    MiniMediaInfo(
                        mediaMetadata = mediaMetadata,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .graphicsLayer { translationX = dragSumX },
                        imageModifier = imageModifier
                    )
                }

                IconButton(
                    enabled = true,
                    onClick = {
                        if (playerState?.isPlaying == true)
                            mediaController?.pause()
                        else
                            mediaController?.play()
                    }
                ) {
                    Icon(
                        imageVector = if (playerState?.isPlaying == true) Pause else PlayArrow,
                        contentDescription = null
                    )
                }
                IconButton(
                    enabled = true,
                    onClick = {
                        mediaController?.seekToNext()
                    }
                ) {
                    Icon(imageVector = SkipNext, contentDescription = null)
                }
            }
        }
}

@Composable
fun MiniMediaInfo(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(modifier = Modifier) {
            AsyncImage(
                model = mediaMetadata.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        ) {
            mediaMetadata.title?.let {
                Text(
                    text = it.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
            }
            mediaMetadata.artist?.let {
                Text(
                    text = it.toString(),
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

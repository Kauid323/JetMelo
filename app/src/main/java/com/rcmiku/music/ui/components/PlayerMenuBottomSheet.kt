package com.rcmiku.music.ui.components

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.ui.icons.Cloud
import com.rcmiku.music.ui.icons.Comment
import com.rcmiku.music.ui.icons.SongListAdd
import com.rcmiku.music.ui.icons.Timelapse
import com.rcmiku.music.ui.icons.Timer
import com.rcmiku.music.viewModel.LyricType
import com.rcmiku.music.viewModel.LyricTypeStore
import com.rcmiku.ncmapi.api.player.PlayerApi
import com.rcmiku.ncmapi.model.Song
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMenuBottomSheet(
    currentSong: Song? = null,
    openBottomSheet: Boolean,
    onDismiss: () -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var timePicker by rememberSaveable { mutableStateOf(false) }
    val playerState = LocalPlayerState.current
    val isSleepTimerSet = playerState?.isSleepTimerSet == true
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var cancelSleepTimer by rememberSaveable { mutableStateOf(false) }
    var openSongListBottomSheet by rememberSaveable { mutableStateOf(false) }
    var openCommentBottomSheet by rememberSaveable { mutableStateOf(false) }
    var openLyricTypeBottomSheet by rememberSaveable { mutableStateOf(false) }
    var lastDownloadId by rememberSaveable { mutableStateOf<Long?>(null) }

    val downloadCompleteReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != -1L && id == lastDownloadId) {
                    Toast.makeText(ctx, "下载完成", Toast.LENGTH_SHORT).show()
                    lastDownloadId = null
                }
            }
        }
    }

    DisposableEffect(openBottomSheet) {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(downloadCompleteReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(downloadCompleteReceiver, filter)
        }
        onDispose {
            runCatching { context.unregisterReceiver(downloadCompleteReceiver) }
        }
    }

    LaunchedEffect(openBottomSheet) {
        if (openBottomSheet) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    if (openBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
        ) {
            LazyColumn(
                Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 8.dp,
                            bottomEnd = 8.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    openCommentBottomSheet = true
                                    onDismiss()
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Comment,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.comment),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    openLyricTypeBottomSheet = true
                                    onDismiss()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lyric",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = "切换歌词",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    if (isSleepTimerSet)
                                        cancelSleepTimer = true
                                    else
                                        timePicker = true
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSleepTimerSet) Timelapse else Timer,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(if (isSleepTimerSet) R.string.remaining_time else R.string.sleep_timer),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (isSleepTimerSet) {
                                playerState?.remainingTime?.let {
                                    Text(
                                        text = it.toDuration(DurationUnit.SECONDS)
                                            .toComponents { hours, minutes, seconds, _ ->
                                                if (hours > 0) {
                                                    "%02dh:%02dm:%02ds".format(
                                                        hours,
                                                        minutes,
                                                        seconds
                                                    )
                                                } else {
                                                    "%02dm:%02ds".format(minutes, seconds)
                                                }
                                            },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    openSongListBottomSheet = true
                                    onDismiss()
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = SongListAdd,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.add_to_songList),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    val songId = currentSong?.id
                                    if (songId == null || songId <= 0L) {
                                        Toast.makeText(context, "没有可下载的歌曲", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }

                                    coroutineScope.launch {
                                        val result = PlayerApi.songPlayUrlV1(id = songId.toString())
                                        val resp = result.getOrNull()
                                        val url = resp?.data
                                            ?.firstOrNull { !it.url.isNullOrBlank() }
                                            ?.url

                                        if (url.isNullOrBlank()) {
                                            Toast.makeText(context, "获取下载链接失败", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }

                                        val songName = currentSong.name ?: songId.toString()
                                        val safeName = songName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                        val ext = Uri.parse(url).lastPathSegment?.substringAfterLast('.', "mp3") ?: "mp3"
                                        val fileName = "$safeName.$ext"

                                        val request = DownloadManager.Request(Uri.parse(url))
                                            .setTitle(songName)
                                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            .setAllowedOverMetered(true)
                                            .setAllowedOverRoaming(true)
                                            .setDestinationInExternalPublicDir(
                                                Environment.DIRECTORY_DOWNLOADS,
                                                "JetMelo/Music/$fileName"
                                            )

                                        val dm = ContextCompat.getSystemService(context, DownloadManager::class.java)
                                        if (dm == null) {
                                            Toast.makeText(context, "下载服务不可用", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }

                                        lastDownloadId = runCatching { dm.enqueue(request) }.getOrNull()
                                        if (lastDownloadId == null) {
                                            Toast.makeText(context, "开始下载失败", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "开始下载", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Cloud,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = "下载歌曲",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 8.dp,
                            topEnd = 8.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable(onClick = {
                                    currentSong?.id?.let {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "https://music.163.com/#/song?id=${it}"
                                            )
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                shareIntent,
                                                context.getString(R.string.share_link)
                                            )
                                        )
                                    }
                                }), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.share),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }


                item {
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (timePicker)
                TimePickerDialog(
                    onDismiss = {
                        timePicker = false
                    }, onTimeSet = {
                        playerState?.startTimer(it)
                        timePicker = false
                    }
                )

            if (cancelSleepTimer) {
                Dialog(
                    onConfirmation = {
                        playerState?.cancelTimer()
                        cancelSleepTimer = false
                    },
                    onDismissRequest = {
                        cancelSleepTimer = false
                    },
                    dialogTitle = stringResource(R.string.sleep_timer_cancel),
                )
            }
        }
    }

    SongListBottomSheet(song = currentSong, onDismiss = {
        openSongListBottomSheet = false
    }, openBottomSheet = openSongListBottomSheet)

    if (openCommentBottomSheet) {
        currentSong?.id?.let { songId ->
            CommentBottomSheet(
                songId = songId,
                onDismiss = { openCommentBottomSheet = false }
            )
        }
    }

    if (openLyricTypeBottomSheet) {
        LyricTypeBottomSheet(
            openBottomSheet = openLyricTypeBottomSheet,
            onDismiss = { openLyricTypeBottomSheet = false },
            onSelect = { type ->
                if (type != LyricType.YRC) {
                    LyricTypeStore.set(type)
                }
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricTypeBottomSheet(
    openBottomSheet: Boolean,
    onDismiss: () -> Unit,
    onSelect: (LyricType) -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(openBottomSheet) {
        if (openBottomSheet) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    if (!openBottomSheet) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
    ) {
        LazyColumn(
            Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 8.dp,
                        bottomEnd = 8.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable {
                                onSelect(LyricType.LYRIC)
                                onDismiss()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "lyric",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable {
                                onSelect(LyricType.KLYRIC)
                                onDismiss()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "klyric",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable {
                                onSelect(LyricType.TLYRIC)
                                onDismiss()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "tlyric",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable {
                                onSelect(LyricType.ROMALRC)
                                onDismiss()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "romalrc",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable {
                                onSelect(LyricType.YTLRC)
                                onDismiss()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ytlrc",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable {
                                onSelect(LyricType.YROMALRC)
                                onDismiss()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "yromalrc",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable {
                                onSelect(LyricType.YRC)
                                onDismiss()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "yrc（暂不支持）",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
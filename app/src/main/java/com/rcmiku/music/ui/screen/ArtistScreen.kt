package com.rcmiku.music.ui.screen

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.extensions.playMediaAtId
import com.rcmiku.music.extensions.setPlaylist
import com.rcmiku.music.ui.components.AlbumListItem
import com.rcmiku.music.ui.components.NavigationTitle
import com.rcmiku.music.ui.components.SongListItem
import com.rcmiku.music.ui.components.SongMenuBottomSheet
import com.rcmiku.music.ui.navigation.AlbumNav
import com.rcmiku.music.ui.navigation.MvPlayerNav
import com.rcmiku.music.viewModel.ArtistScreenViewModel
import com.rcmiku.ncmapi.model.Song

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ArtistScreen(
    navController: NavHostController,
    artistScreenViewModel: ArtistScreenViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    val artistHeadInfoState by artistScreenViewModel.artistHeadInfo.collectAsState()
    val artistTopSongState by artistScreenViewModel.artistTopSong.collectAsState()
    val artistDescState by artistScreenViewModel.artistDesc.collectAsState()
    val artistAlbumList = artistScreenViewModel.artistAlbumList.collectAsLazyPagingItems()
    val artistMvList = artistScreenViewModel.artistMvList.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val showPlaylistTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    var state by rememberSaveable { mutableIntStateOf(1) }

    LaunchedEffect(state) {
        if (state == 3) {
            artistMvList.refresh()
        }
    }
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId?.toLongOrNull()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectSong by remember { mutableStateOf<Song?>(null) }

    val titles = listOf(
        stringResource(R.string.home),
        stringResource(R.string.song),
        stringResource(R.string.album),
        stringResource(R.string.mv)
    )

    with(sharedTransitionScope) {
        LazyColumn(state = listState) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomStart
            ) {
                AsyncImage(
                    model = artistHeadInfoState?.data?.artist?.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.aspectRatio(4f / 3f)
                )
                artistHeadInfoState?.data?.artist?.name?.let {
                    Box(
                        Modifier
                            .padding(6.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(color = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = it,
                            Modifier.padding(4.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
        item {
            Column {
                SecondaryTabRow(selectedTabIndex = state) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = state == index,
                            onClick = { state = index },
                            text = {
                                Text(
                                    text = title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }

        when (state) {
            0 -> {
                item {
                    NavigationTitle(
                        title = stringResource(R.string.artist_info),
                        modifier = Modifier.animateItem()
                    )
                    val descText = (artistDescState?.briefDesc ?: artistHeadInfoState?.data?.artist?.briefDesc)
                        ?.trimIndent()
                    descText?.let {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            if (it.isNotBlank())
                                Text(
                                    text = it,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            else
                                Text(
                                    text = stringResource(R.string.no_brief),
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                        }
                    }
                }
            }

            1 -> {
                artistTopSongState?.songs?.let {
                    itemsIndexed(it) { index, song ->
                        SongListItem(
                            song = song,
                            isPlaying = isPlaying,
                            isActive = currentMediaId == song.id,
                            songIndex = index + 1,
                            modifier = Modifier.clickable {
                                mediaController?.setPlaylist(it)
                                mediaController?.playMediaAtId(song.id)
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    selectSong = song
                                    openBottomSheet = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.more)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            2 -> {
                items(artistAlbumList.itemCount) { index ->
                    artistAlbumList[index]?.let {
                        AlbumListItem(album = it, modifier = Modifier.clickable {
                            navController.navigate(AlbumNav(albumId = it.id))
                        })
                    }
                }
            }

            3 -> {
                items(artistMvList.itemCount) { index ->
                    val record = artistMvList[index]
                    val base = record?.resource?.mlogBaseData
                    val ext = record?.resource?.mlogExtVO
                    val coverUrl = base?.coverUrl
                    val title = base?.text ?: base?.desc ?: ""
                    val author = ext?.artistName
                        ?: ext?.artists?.firstOrNull()?.name
                        ?: ""

                    fun normalizeVideoUrl(raw: String?): String? {
                        if (raw.isNullOrBlank()) return null
                        if (raw.startsWith("http")) return raw
                        return "https://music.163.com/" + raw.trimStart('/')
                    }

                    fun parseMvId(): Long {
                        val threadId = base?.threadId
                        if (!threadId.isNullOrBlank()) {
                            // e.g. R_MV_5_14679928
                            val last = threadId.split("_").lastOrNull()
                            last?.toLongOrNull()?.let { return it }
                        }
                        return base?.id?.toLongOrNull() ?: 0L
                    }

                    val bestFromVideoInfo = base?.video?.urlInfos
                        ?.filter { !it.url.isNullOrBlank() }
                        ?.maxByOrNull { it.r ?: 0 }
                        ?.url
                        ?: base?.video?.urlInfo?.url

                    val bestFromVideos = base?.videos
                        ?.firstOrNull { !it.url.isNullOrBlank() }
                        ?.url

                    val videoUrl = normalizeVideoUrl(bestFromVideoInfo ?: bestFromVideos)
                    val sharedKey = record?.id ?: ""
                    val mvId = parseMvId()

                    if (record != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(
                                        MvPlayerNav(
                                            mvId = mvId,
                                            videoUrl = "",
                                            sharedKey = sharedKey,
                                            title = title,
                                            author = author,
                                            coverUrl = coverUrl ?: ""
                                        )
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .sharedElement(
                                        state = sharedTransitionScope.rememberSharedContentState(
                                            key = "mv_cover_" + sharedKey
                                        ),
                                        animatedVisibilityScope = animatedContentScope,
                                        boundsTransform = AlbumArtBoundsTransform,
                                    )
                            )

                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 14.sp,
                                    modifier = Modifier.sharedElement(
                                        state = sharedTransitionScope.rememberSharedContentState(
                                            key = "mv_title_" + sharedKey
                                        ),
                                        animatedVisibilityScope = animatedContentScope,
                                        boundsTransform = AlbumArtBoundsTransform,
                                    )
                                )
                                Text(
                                    text = author,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

            item {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    TopAppBar(
        title = {
            Text(
                text = if (showPlaylistTitle) artistHeadInfoState?.data?.artist?.name ?: "" else "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.navigateUp()
                },
                colors = if (showPlaylistTitle) IconButtonDefaults.iconButtonColors() else IconButtonDefaults.filledIconButtonColors()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }
        },
        modifier = Modifier.background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    Color.Transparent
                )
            )
        ),
        colors = if (showPlaylistTitle) TopAppBarDefaults.topAppBarColors() else TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )

    SongMenuBottomSheet(
        navController = navController,
        song = selectSong,
        onDismiss = { openBottomSheet = false },
        openBottomSheet = openBottomSheet
    )
}

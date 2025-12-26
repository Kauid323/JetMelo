package com.rcmiku.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rcmiku.music.data.model.comment.Comment
import com.rcmiku.music.ui.viewmodel.CommentViewModel
import com.rcmiku.ncmapi.model.UserDetailResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    songId: Long,
    onDismiss: () -> Unit,
    viewModel: CommentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    LaunchedEffect(songId) {
        viewModel.loadComments(songId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header: Title and Sort Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "评论 ${uiState.totalCount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortChip(
                        selected = uiState.sortType == 1,
                        label = "Rec.",
                        onClick = { viewModel.updateSortType(1) }
                    )
                    SortChip(
                        selected = uiState.sortType == 2,
                        label = "Hot",
                        onClick = { viewModel.updateSortType(2) }
                    )
                    SortChip(
                        selected = uiState.sortType == 3,
                        label = "New",
                        onClick = { viewModel.updateSortType(3) }
                    )
                }
            }

            if (uiState.isLoading && uiState.comments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                CommentList(
                    comments = uiState.comments,
                    onLoadMore = { viewModel.loadComments(songId) },
                    onClickComment = { viewModel.openFloor(it) },
                    onToggleLike = { viewModel.toggleLike(it) },
                    onClickAvatar = { userId -> viewModel.loadUserProfile(userId) }
                )
            }
        }
    }

    val floorParent = uiState.floorParent
    if (floorParent != null) {
        FloorCommentBottomSheet(
            parent = floorParent,
            comments = uiState.floorComments,
            isLoading = uiState.floorIsLoading,
            hasMore = uiState.floorHasMore,
            onLoadMore = { viewModel.loadFloorMore() },
            onDismiss = { viewModel.closeFloor() }
        )
    }

    if (uiState.userProfileLoading || uiState.userProfile != null) {
        UserProfileBottomSheet(
            user = uiState.userProfile,
            loading = uiState.userProfileLoading,
            onDismiss = { viewModel.closeUserProfile() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileBottomSheet(
    user: UserDetailResponse?,
    loading: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxSize()
    ) {
        if (loading || user?.profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@ModalBottomSheet
        }

        val profile = user.profile!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = profile.nickname,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "UID: ${profile.userId}  Lv.${user.level}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "听歌 ${user.listenSongs}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val sig = profile.signature
            if (!sig.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = sig,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SortChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun CommentList(
    comments: List<Comment>,
    onLoadMore: () -> Unit,
    onClickComment: (Comment) -> Unit,
    onToggleLike: (Comment) -> Unit,
    onClickAvatar: (Long) -> Unit
) {
    val listState = rememberLazyListState()

    // Simple infinite scroll detection
    val isAtBottom = !listState.canScrollForward
    LaunchedEffect(isAtBottom) {
        if (isAtBottom && comments.isNotEmpty()) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(comments) { comment ->
            CommentItem(
                comment = comment,
                onClick = { onClickComment(comment) },
                onToggleLike = { onToggleLike(comment) },
                onClickAvatar = { onClickAvatar(comment.user.userId) }
            )
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    onClick: (() -> Unit)? = null,
    onToggleLike: (() -> Unit)? = null,
    onClickAvatar: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { base ->
                if (onClick != null) {
                    base.clickable { onClick() }
                } else {
                    base
                }
            }
    ) {
        // Avatar
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(comment.user.avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Gray)
                .let { base -> if (onClickAvatar != null) base.clickable { onClickAvatar() } else base }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Nickname and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = comment.user.nickname,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Like count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.let { base ->
                        if (onToggleLike != null) base.clickable { onToggleLike() } else base
                    }
                ) {
                    Text(
                        text = comment.likedCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        tint = if (comment.liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            Text(
                text = formatTime(comment.time),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Content
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Be Replied
            if (!comment.beReplied.isNullOrEmpty()) {
                val replied = comment.beReplied[0]
                if (replied.beRepliedCommentId != comment.parentCommentId) {
                     Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "@${replied.user.nickname}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                         Text(
                            text = replied.content ?: "Comment deleted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(time: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(time))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloorCommentBottomSheet(
    parent: Comment,
    comments: List<Comment>,
    isLoading: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val isAtBottom = !listState.canScrollForward

    LaunchedEffect(isAtBottom, hasMore, isLoading, comments.size) {
        if (isAtBottom && hasMore && !isLoading && comments.isNotEmpty()) {
            onLoadMore()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "楼中楼",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            CommentItem(comment = parent)

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(comments) { c ->
                    CommentItem(comment = c)
                }
                item {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

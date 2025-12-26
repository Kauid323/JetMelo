package com.rcmiku.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.music.data.model.comment.Comment
import com.rcmiku.music.data.repository.CommentRepository
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.model.UserDetailResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommentUiState(
    val isLoading: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = true,
    val sortType: Int = 1, // 1: Recommended, 2: Hot, 3: Time
    val error: String? = null,
    val currentSongId: Long = 0L,
    val floorParent: Comment? = null,
    val floorComments: List<Comment> = emptyList(),
    val floorHasMore: Boolean = true,
    val floorIsLoading: Boolean = false,
    val floorTime: Long = -1,
    val userProfile: UserDetailResponse? = null,
    val userProfileLoading: Boolean = false
)

@HiltViewModel
class CommentViewModel @Inject constructor(
    private val commentRepository: CommentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentUiState())
    val uiState: StateFlow<CommentUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var currentCursor = 0L

    private var floorTime = -1L

    fun toggleLike(comment: Comment) {
        val songId = _uiState.value.currentSongId
        if (songId == 0L) return

        val willLike = !comment.liked
        // Optimistic UI update
        _uiState.update { state ->
            state.copy(
                comments = state.comments.map { c ->
                    if (c.commentId != comment.commentId) c
                    else c.copy(
                        liked = willLike,
                        likedCount = (c.likedCount + if (willLike) 1 else -1).coerceAtLeast(0)
                    )
                }
            )
        }

        viewModelScope.launch {
            val res = commentRepository.likeComment(
                id = songId,
                commentId = comment.commentId,
                t = if (willLike) 1 else 0,
                type = 0
            )
            res.onFailure {
                // Revert on failure
                _uiState.update { state ->
                    state.copy(
                        comments = state.comments.map { c ->
                            if (c.commentId != comment.commentId) c
                            else c.copy(
                                liked = comment.liked,
                                likedCount = comment.likedCount
                            )
                        }
                    )
                }
            }
        }
    }

    fun loadUserProfile(userId: Long) {
        if (userId == 0L) return
        viewModelScope.launch {
            _uiState.update { it.copy(userProfileLoading = true) }
            val res = AccountApi.userDetail(userId).getOrNull()
            _uiState.update { it.copy(userProfile = res, userProfileLoading = false) }
        }
    }

    fun closeUserProfile() {
        _uiState.update { it.copy(userProfile = null, userProfileLoading = false) }
    }

    fun loadComments(songId: Long, isRefresh: Boolean = false) {
        if (songId != _uiState.value.currentSongId || isRefresh) {
            _uiState.update {
                it.copy(
                    currentSongId = songId,
                    comments = emptyList(),
                    isLoading = true,
                    totalCount = 0,
                    hasMore = true,
                    floorParent = null,
                    floorComments = emptyList(),
                    floorHasMore = true,
                    floorIsLoading = false,
                    floorTime = -1
                )
            }
            currentPage = 1
            currentCursor = 0L
            floorTime = -1L
        }

        if (!_uiState.value.hasMore && !isRefresh) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Adjust params based on sort type logic from Vutron
            // If sortType is 1 (Recommendation), it usually only has one page, 
            // then we might need to switch to Time (3) for pagination if we want "all" comments behavior 
            // but for simplicity let's implement basic sorting first.
            
            val result = commentRepository.getComments(
                id = songId,
                type = 0, // Song
                pageNo = currentPage,
                sortType = _uiState.value.sortType,
                cursor = currentCursor
            )

            result.fold(
                onSuccess = { commentResult ->
                    val data = commentResult.data
                    if (data != null) {
                        _uiState.update { state ->
                            state.copy(
                                comments = if (currentPage == 1) data.comments else state.comments + data.comments,
                                totalCount = data.totalCount,
                                hasMore = data.hasMore,
                                isLoading = false
                            )
                        }
                        currentPage++
                        currentCursor = data.cursor
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    fun updateSortType(sortType: Int) {
        if (_uiState.value.sortType == sortType) return
        _uiState.update { it.copy(sortType = sortType) }
        loadComments(_uiState.value.currentSongId, isRefresh = true)
    }

    fun openFloor(parent: Comment) {
        _uiState.update {
            it.copy(
                floorParent = parent,
                floorComments = emptyList(),
                floorHasMore = true,
                floorIsLoading = false,
                floorTime = -1,
                error = null
            )
        }
        floorTime = -1L
        loadFloorMore()
    }

    fun closeFloor() {
        _uiState.update {
            it.copy(
                floorParent = null,
                floorComments = emptyList(),
                floorHasMore = true,
                floorIsLoading = false,
                floorTime = -1
            )
        }
        floorTime = -1L
    }

    fun loadFloorMore(limit: Int = 30) {
        val songId = _uiState.value.currentSongId
        val parent = _uiState.value.floorParent ?: return
        if (_uiState.value.floorIsLoading) return
        if (!_uiState.value.floorHasMore && _uiState.value.floorComments.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(floorIsLoading = true, error = null) }
            val result = commentRepository.getFloorComments(
                parentCommentId = parent.commentId,
                id = songId,
                type = 0,
                limit = limit,
                time = floorTime
            )

            result.fold(
                onSuccess = { commentResult ->
                    val data = commentResult.data
                    if (data != null) {
                        _uiState.update { state ->
                            val merged = state.floorComments + data.comments
                            state.copy(
                                floorComments = merged,
                                floorHasMore = data.hasMore,
                                floorIsLoading = false,
                                floorTime = data.cursor
                            )
                        }
                        floorTime = data.cursor
                    } else {
                        _uiState.update { it.copy(floorIsLoading = false) }
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(floorIsLoading = false, error = error.message) }
                }
            )
        }
    }
}

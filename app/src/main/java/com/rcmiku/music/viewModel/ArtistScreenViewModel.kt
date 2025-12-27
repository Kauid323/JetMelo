package com.rcmiku.music.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.rcmiku.music.paging.ArtistAlbumPagingSource
import com.rcmiku.music.paging.ArtistMvPagingSource
import com.rcmiku.ncmapi.api.artist.ArtistApi
import com.rcmiku.ncmapi.model.ArtistDescResponse
import com.rcmiku.ncmapi.model.ArtistHeadInfoResponse
import com.rcmiku.ncmapi.model.ArtistTopSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistScreenViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val artistId: Long =
        savedStateHandle.get<Long>("artistId")
            ?: savedStateHandle.get<String>("artistId")?.toLongOrNull()
            ?: savedStateHandle.get<Int>("artistId")?.toLong()
            ?: 0L

    private val _artistHeadInfo =
        MutableStateFlow<ArtistHeadInfoResponse?>(null)
    val artistHeadInfo: StateFlow<ArtistHeadInfoResponse?> =
        _artistHeadInfo.asStateFlow()
    private val _artistTopSong =
        MutableStateFlow<ArtistTopSong?>(null)
    val artistTopSong: StateFlow<ArtistTopSong?> =
        _artistTopSong.asStateFlow()

    private val _artistDesc = MutableStateFlow<ArtistDescResponse?>(null)
    val artistDesc: StateFlow<ArtistDescResponse?> = _artistDesc.asStateFlow()

    init {
        viewModelScope.launch {
            if (artistId > 0L) {
                _artistHeadInfo.value = ArtistApi.artistHeadInfo(artistId).getOrNull()
                _artistTopSong.value = ArtistApi.artistTopSong(artistId).getOrNull()
                _artistDesc.value = ArtistApi.artistDesc(artistId).getOrNull()
            }
        }
    }

    val artistAlbumList = if (artistId > 0L) {
        Pager(
            config = PagingConfig(
                pageSize = 100,
                prefetchDistance = 50,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { ArtistAlbumPagingSource(artistId) }
        ).flow.cachedIn(viewModelScope)
    } else {
        flowOf()
    }

    val artistMvList = if (artistId > 0L) {
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { ArtistMvPagingSource(artistId) }
        ).flow.cachedIn(viewModelScope)
    } else {
        flowOf()
    }

}
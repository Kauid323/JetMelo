package com.rcmiku.music.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.ncmapi.api.player.MvApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MvPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val mvId: Long = savedStateHandle.get<Long>("mvId")
        ?: savedStateHandle.get<String>("mvId")?.toLongOrNull()
        ?: 0L

    val videoUrlArg: String = savedStateHandle.get<String>("videoUrl") ?: ""
    val sharedKey: String = savedStateHandle.get<String>("sharedKey") ?: ""
    val title: String = savedStateHandle.get<String>("title") ?: ""
    val author: String = savedStateHandle.get<String>("author") ?: ""
    val coverUrl: String = savedStateHandle.get<String>("coverUrl") ?: ""

    private val _playUrl = MutableStateFlow<String?>(null)
    val playUrl: StateFlow<String?> = _playUrl.asStateFlow()

    init {
        viewModelScope.launch {
            if (videoUrlArg.isNotBlank()) {
                _playUrl.value = videoUrlArg
                return@launch
            }
            if (mvId > 0L) {
                val resp = MvApi.mvUrl(id = mvId).getOrNull()
                _playUrl.value = resp?.data?.url
            }
        }
    }
}

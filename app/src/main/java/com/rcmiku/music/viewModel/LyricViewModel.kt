package com.rcmiku.music.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.ncmapi.api.player.PlayerApi
import com.rcmiku.ncmapi.model.LyricResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricViewModel @Inject constructor() : ViewModel() {

    private val _lyric = MutableStateFlow<LyricResponse?>(null)
    val lyric: StateFlow<LyricResponse?> = _lyric.asStateFlow()

    private val _lyricType = MutableStateFlow(LyricType.LYRIC)
    val lyricType: StateFlow<LyricType> = _lyricType.asStateFlow()

    fun fetchLyric(musicId: Long) {
        viewModelScope.launch {
            _lyric.value = PlayerApi.songLyric(musicId).getOrNull()
            _lyricType.value = LyricType.LYRIC
            LyricTypeStore.set(LyricType.LYRIC)
        }
    }

    fun setLyricType(type: LyricType) {
        _lyricType.value = type
        LyricTypeStore.set(type)
    }
}

enum class LyricType {
    LYRIC,
    KLYRIC,
    TLYRIC,
    ROMALRC,
    YTLRC,
    YROMALRC,
    YRC
}
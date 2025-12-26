package com.rcmiku.music.viewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LyricTypeStore {
    private val _type = MutableStateFlow(LyricType.LYRIC)
    val type: StateFlow<LyricType> = _type.asStateFlow()

    fun set(type: LyricType) {
        _type.value = type
    }
}

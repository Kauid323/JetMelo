package com.rcmiku.music.viewModel

import android.content.Context
import com.rcmiku.music.constants.lyricTypeKey
import com.rcmiku.music.utils.dataStore
import com.rcmiku.music.utils.toEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit

object LyricTypeStore {
    private val _type = MutableStateFlow(LyricType.LYRIC)
    val type: StateFlow<LyricType> = _type.asStateFlow()

    private var initialized = false
    private var appContext: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        scope.launch {
            val prefs = appContext!!.dataStore.data.first()
            val stored = prefs[lyricTypeKey].toEnum(LyricType.LYRIC)
            _type.value = stored
        }
    }

    fun set(type: LyricType) {
        _type.value = type

        val ctx = appContext
        if (ctx != null) {
            scope.launch {
                ctx.dataStore.edit { it[lyricTypeKey] = type.name }
            }
        }
    }
}

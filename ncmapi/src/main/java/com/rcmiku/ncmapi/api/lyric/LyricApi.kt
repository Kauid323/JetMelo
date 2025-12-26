package com.rcmiku.ncmapi.api.lyric

import com.rcmiku.ncmapi.model.LyricResponse

object LyricApi {
    suspend fun songLyric(id: Long): Result<LyricResponse> {
        return Result.failure(NotImplementedError())
    }
}

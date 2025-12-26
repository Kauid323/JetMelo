package com.rcmiku.ncmapi.api.album

import com.rcmiku.ncmapi.model.AlbumDetailResponse
import com.rcmiku.ncmapi.model.AlbumInfoResponse

object AlbumApi {
    suspend fun albumDetail(id: Long): Result<AlbumDetailResponse> {
        return Result.failure(NotImplementedError())
    }

    suspend fun albumInfo(id: Long): Result<AlbumInfoResponse> {
        return Result.failure(NotImplementedError())
    }

    suspend fun albumSub(id: Long, isSub: Boolean): Result<Any> {
        return Result.failure(NotImplementedError())
    }
}

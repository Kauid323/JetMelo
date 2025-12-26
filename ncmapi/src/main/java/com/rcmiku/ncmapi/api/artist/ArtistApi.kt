package com.rcmiku.ncmapi.api.artist

import com.rcmiku.ncmapi.model.ArtistAlbumResponse
import com.rcmiku.ncmapi.model.ArtistHeadInfoResponse
import com.rcmiku.ncmapi.model.ArtistTopSong

object ArtistApi {
    suspend fun artistHeadInfo(id: Long): Result<ArtistHeadInfoResponse> {
        return Result.failure(NotImplementedError())
    }
    
    suspend fun artistTopSong(id: Long): Result<ArtistTopSong> {
        return Result.failure(NotImplementedError())
    }
    
    suspend fun artistAlbum(id: Long, limit: Int = 30, offset: Int = 0): Result<ArtistAlbumResponse> {
        return Result.failure(NotImplementedError())
    }
}

package com.rcmiku.music.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rcmiku.ncmapi.api.artist.ArtistMvApi
import com.rcmiku.ncmapi.model.ArtistVideoRecord

class ArtistMvPagingSource(private val artistId: Long) : PagingSource<String, ArtistVideoRecord>() {
    override fun getRefreshKey(state: PagingState<String, ArtistVideoRecord>): String? {
        return null
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ArtistVideoRecord> {
        return try {
            val cursor = params.key ?: "0"
            val size = params.loadSize
            val response = ArtistMvApi.artistVideo(artistId = artistId, size = size, cursor = cursor)
            if (response.isSuccess) {
                val body = response.getOrThrow()
                val data = body.data?.records.orEmpty()
                val page = body.data?.page
                Log.w("ArtistMvPaging", "artistId=$artistId cursor=$cursor size=$size records=${data.size} more=${page?.more} nextCursor=${page?.cursor}")
                val nextKey = if (page?.more == true) page.cursor else null
                LoadResult.Page(
                    data = data,
                    prevKey = null,
                    nextKey = nextKey
                )
            } else {
                LoadResult.Error(Exception("Load data filed"))
            }
        } catch (e: Exception) {
            Log.e("ArtistMvPaging", "load failed artistId=$artistId", e)
            LoadResult.Error(e)
        }
    }
}

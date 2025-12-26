package com.rcmiku.ncmapi.api.search

import com.rcmiku.ncmapi.model.SearchResponse
import com.rcmiku.ncmapi.model.SearchSuggestKeywordResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json

object SearchApi {
    suspend fun search(offset: Int = 0, limit: Int = 30, keyword: String, searchType: SearchType = SearchType.Song): Result<SearchResponse> {
        return runCatching {
            val (url, data) = when (searchType) {
                SearchType.Song -> "/api/search/resource/horizontal/song" to mapOf(
                    "offset" to offset.toString(),
                    "limit" to limit.toString(),
                    "keyword" to keyword,
                    "header" to "{}",
                    "e_r" to false
                )
                SearchType.Playlist -> "/api/search/multi/terminal/playlist/get" to mapOf(
                    "offset" to offset.toString(),
                    "limit" to limit.toString(),
                    "keyword" to keyword,
                    "header" to "{}",
                    "e_r" to false
                )
                else -> "/api/search/get" to mapOf(
                    "s" to keyword,
                    "type" to searchType.type,
                    "limit" to limit,
                    "offset" to offset
                )
            }
            val body = HttpManager.request(
                url = url,
                data = data,
                crypto = if (searchType == SearchType.Song || searchType == SearchType.Playlist) HttpManager.CryptoType.EAPI else HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(SearchResponse.serializer(), body)
        }
    }
    
    suspend fun searchSuggestKeyword(keywords: String): Result<SearchSuggestKeywordResponse> {
        return runCatching {
            // ref: my-netease-cloud-music-api module/search_suggest.js => /api/search/suggest/web
            val body = HttpManager.request(
                url = "/api/search/suggest/web",
                data = mapOf(
                    "s" to keywords
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(SearchSuggestKeywordResponse.serializer(), body)
        }
    }
}

package com.rcmiku.ncmapi.api.playlist

import android.util.Log
import com.rcmiku.ncmapi.model.PlaylistDetailResponse
import com.rcmiku.ncmapi.model.PlaylistInfoResponse
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.TopListResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.jsonObject

object PlaylistApi {
    private const val TAG = "PlaylistApi"

    suspend fun playlistDetail(id: Long): Result<PlaylistDetailResponse> {
        return playlistV3Detail(id)
    }

    suspend fun playlistDetail(id: Long, limit: Int): Result<PlaylistDetailResponse> {
        // For large playlists (e.g. liked songs), v6 EAPI behaves closer to official client.
        return playlistV6DetailEapi(id = id, n = limit, s = 5)
    }

    suspend fun playlistV3Detail(id: Long): Result<PlaylistDetailResponse> {
        return runCatching {
            Log.w(TAG, "playlistV3Detail request id=$id")
            val body = HttpManager.request(
                url = "/weapi/v3/playlist/detail",
                data = mapOf(
                    "id" to id.toString(),
                    "n" to "100000",
                    "s" to "8"
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(PlaylistDetailResponse.serializer(), body)
        }.recoverCatching { e ->
            Log.w(TAG, "playlistV3Detail failed, fallback to v6. id=$id", e)
            playlistV6Detail(id).getOrThrow()
        }
    }

    suspend fun playlistV6Detail(id: Long): Result<PlaylistDetailResponse> {
        return runCatching {
            // ref: module/playlist_detail.js => /api/v6/playlist/detail
            Log.w(TAG, "playlistV6Detail request id=$id")
            val body = HttpManager.request(
                url = "/api/v6/playlist/detail",
                data = mapOf(
                    "id" to id.toString(),
                    "n" to "100000",
                    "s" to "8"
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(PlaylistDetailResponse.serializer(), body)
        }
    }

    suspend fun playlistV6DetailEapi(id: Long, n: Int = 1000, s: Int = 5): Result<PlaylistDetailResponse> {
        return runCatching {
            // EAPI for /api/v6/playlist/detail (liked songs playlist works reliably here)
            Log.w(TAG, "playlistV6DetailEapi request id=$id n=$n s=$s")
            val body = HttpManager.request(
                url = "/api/v6/playlist/detail",
                data = mapOf(
                    "id" to id.toString(),
                    "n" to n.toString(),
                    "s" to s.toString(),
                    "t" to (System.currentTimeMillis() / 1000).toString(),
                    "header" to "{}",
                    "e_r" to false
                ),
                crypto = HttpManager.CryptoType.EAPI
            )
            json.decodeFromString(PlaylistDetailResponse.serializer(), body)
        }
    }

    suspend fun playlistSub(id: Long, isSub: Boolean): Result<Any> {
        return runCatching {
            // ref: module/playlist_subscribe.js => /api/playlist/subscribe|unsubscribe
            HttpManager.request(
                url = "/weapi/playlist/subscribe",
                data = mapOf(
                    "id" to id.toString(),
                    "t" to (if (isSub) "1" else "2")
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )
        }
    }

    suspend fun topList(): Result<TopListResponse> {
        return runCatching {
            val body = HttpManager.request(
                url = "/api/toplist",
                data = emptyMap(),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(TopListResponse.serializer(), body)
        }
    }
    
    suspend fun playlistInfo(id: Long): Result<PlaylistInfoResponse> {
         return runCatching {
             // Use same detail endpoint and map into PlaylistInfoResponse
             val detail = playlistDetail(id).getOrThrow()
             PlaylistInfoResponse(playlist = detail.playlist)
         }
    }

    suspend fun playlistTrackAll(id: Long, limit: Int = 1000, offset: Int = 0, s: Int = 8): Result<PlaylistDetailResponse> {
        return runCatching {
            val detail = playlistV3Detail(id).getOrThrow()
            val trackIds = detail.playlist.trackIds
            if (trackIds.isEmpty()) {
                return@runCatching detail
            }

            val slice = trackIds.drop(offset).take(limit)
            val c = "[" + slice.joinToString(",") { "{\"id\":${it.id}}" } + "]"
            val body = HttpManager.request(
                url = "/weapi/v3/song/detail",
                data = mapOf(
                    "c" to c
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )

            // response shape: { songs: [...], privileges: [...] }
            val root = json.parseToJsonElement(body).jsonObject
            val songsJson = root["songs"]
            val songs = if (songsJson != null) {
                json.decodeFromJsonElement(ListSerializer(Song.serializer()), songsJson)
            } else {
                emptyList()
            }

            detail.copy(playlist = detail.playlist.copy(tracks = songs))
        }
    }
}

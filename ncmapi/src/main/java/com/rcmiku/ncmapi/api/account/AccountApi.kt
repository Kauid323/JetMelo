package com.rcmiku.ncmapi.api.account

import com.rcmiku.ncmapi.model.FavoriteSongResponse
import com.rcmiku.ncmapi.model.RecordResponse
import com.rcmiku.ncmapi.model.UserDetailResponse
import com.rcmiku.ncmapi.model.UserInfoBatch
import com.rcmiku.ncmapi.model.UserPlaylistData
import com.rcmiku.ncmapi.model.UserPlaylistResponse
import com.rcmiku.ncmapi.utils.HttpManager
import com.rcmiku.ncmapi.utils.json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object AccountApi {
    suspend fun checkMusic(): Result<Any> {
         return Result.failure(NotImplementedError())
    }
    
    suspend fun getSubcount(): Result<Any> {
         return runCatching {
             // ref: module/user_subcount.js => /api/subcount
             HttpManager.request(
                 url = "/api/subcount",
                 data = emptyMap(),
                 crypto = HttpManager.CryptoType.WEAPI
             )
         }
    }
    
     suspend fun getUserPlaylist(uid: Long, offset: Int = 0, limit: Int = 30): Result<UserPlaylistResponse> {
         return runCatching {
             val body = HttpManager.request(
                 url = "/weapi/user/playlist",
                 data = mapOf(
                     "uid" to uid.toString(),
                     "limit" to limit.toString(),
                     "offset" to offset.toString(),
                     "includeVideo" to "true"
                 ),
                 crypto = HttpManager.CryptoType.WEAPI
             )
            val raw = json.decodeFromString(UserPlaylistResponse.serializer(), body)
            // Ensure data wrapper is always populated for UI.
            raw.copy(data = UserPlaylistData(playlist = if (raw.playlist.isNotEmpty()) raw.playlist else raw.data.playlist))
         }
     }
    
     suspend fun userPlaylist(userId: Long, userPlaylistType: UserPlaylistType): Result<UserPlaylistResponse> {
         return runCatching {
             val raw = getUserPlaylist(uid = userId, offset = 0, limit = 1000).getOrThrow()
             val list = when (userPlaylistType) {
                 UserPlaylistType.CREATE -> raw.data.playlist.filter { it.subscribed.not() }
                 UserPlaylistType.COLLECT -> raw.data.playlist.filter { it.subscribed }
             }
             raw.copy(data = UserPlaylistData(playlist = list), playlist = list)
         }
     }
    
     suspend fun getLikelist(uid: Long): Result<FavoriteSongResponse> = favoriteSong(userId = uid)

     suspend fun favoriteSongIds(): Result<FavoriteSongResponse> = favoriteSong(userId = 0)

     suspend fun favoriteSong(userId: Long): Result<FavoriteSongResponse> {
        return runCatching {
            // EAPI endpoint returns the user's "Liked Songs" playlist object.
            // ref: captured official request => /api/user/playlist/favorite
            val body = HttpManager.request(
                url = "/api/user/playlist/favorite",
                data = mapOf(
                    "userId" to userId.toString(),
                    "t" to (System.currentTimeMillis() / 1000).toString(),
                    "header" to "{}",
                    "e_r" to false
                ),
                crypto = HttpManager.CryptoType.EAPI
            )
            json.decodeFromString(FavoriteSongResponse.serializer(), body)
        }
    }

     suspend fun accountInfo(): Result<UserInfoBatch> {
        return runCatching {
            // Use batch to fetch user detail + level.
            // ref: module/batch.js + user_detail.js + user_level.js
            // We don't know uid here; server uses cookie session to resolve /api/user/account.
            val body = HttpManager.request(
                url = "/weapi/batch",
                data = mapOf(
                    "/api/nuser/account/get" to "{}",
                    "/api/user/level" to "{}"
                ),
                crypto = HttpManager.CryptoType.WEAPI
            )

            val root = json.parseToJsonElement(body).jsonObject
            // Observed response: {"/api/nuser/account/get":{...},"/api/user/level":{...},"code":200}
            val accountResp = root["/api/nuser/account/get"]?.jsonObject
                ?: error("batch missing /api/nuser/account/get")
            val levelResp = root["/api/user/level"]?.jsonObject
                ?: error("batch missing /api/user/level")

            val account = json.decodeFromJsonElement(
                com.rcmiku.ncmapi.model.Account.serializer(),
                accountResp["account"] ?: error("/api/nuser/account/get missing account")
            )
            val profile = runCatching {
                val p = accountResp["profile"]
                if (p != null) json.decodeFromJsonElement(com.rcmiku.ncmapi.model.Profile.serializer(), p) else null
            }.getOrNull()
            val level = runCatching {
                json.decodeFromJsonElement(com.rcmiku.ncmapi.model.UserLevel.serializer(), levelResp)
            }.getOrNull()

            UserInfoBatch(account = account, profile = profile, level = level)
        }
    }

     suspend fun account(): Result<UserInfoBatch> = accountInfo()

    suspend fun userDetail(uid: Long): Result<UserDetailResponse> {
        return runCatching {
            // ref: my-netease-cloud-music-api module/user_detail.js => /api/v1/user/detail/{uid}
            val body = HttpManager.request(
                url = "/api/v1/user/detail/$uid",
                data = emptyMap(),
                crypto = HttpManager.CryptoType.WEAPI
            )
            json.decodeFromString(UserDetailResponse.serializer(), body)
        }
    }

     suspend fun favoriteSongLikeChange(): Result<Any> {
         return Result.failure(NotImplementedError())
     }

     suspend fun songRecord(uid: Long, type: SongRecordType = SongRecordType.WEEK): Result<RecordResponse> {
         return runCatching {
             // ref: module/user_record.js => /api/v1/play/record
             val body = HttpManager.request(
                 url = "/weapi/v1/play/record",
                 data = mapOf(
                     "uid" to uid.toString(),
                     "type" to type.type.toString()
                 ),
                 crypto = HttpManager.CryptoType.WEAPI
             )
             json.decodeFromString(RecordResponse.serializer(), body)
         }
     }

     suspend fun songLike(like: Boolean, songId: Long): Result<Any> {
        return runCatching {
            // EAPI required for /api/song/like. WEAPI often returns 400 (invalid params).
            // Captured payload shows additional fields (userActionMap/t/etc.) even when userid=0.
            val body = HttpManager.request(
                url = "/api/song/like",
                data = mapOf(
                    "like" to like.toString(),
                    "trackId" to songId.toString(),
                    "userActionMap" to "MusicApp||0",
                    "checkToken" to "",
                    "userid" to "0",
                    "rqRefer" to "",
                    "t" to (System.currentTimeMillis() / 1000).toString(),
                    "header" to "{}",
                    "e_r" to false
                ),
                crypto = HttpManager.CryptoType.EAPI
            )
            body
        }
    }

     suspend fun albumSublist(offset: Int, limit: Int): Result<com.rcmiku.ncmapi.model.AlbumSublistResponse> {
          return runCatching {
              val body = HttpManager.request(
                  url = "/api/album/sublist",
                  data = mapOf(
                      "limit" to limit,
                      "offset" to offset,
                      "total" to true
                  ),
                  crypto = HttpManager.CryptoType.WEAPI
              )
              json.decodeFromString(com.rcmiku.ncmapi.model.AlbumSublistResponse.serializer(), body)
          }
     }

     suspend fun cloudSong(offset: Int, limit: Int): Result<com.rcmiku.ncmapi.model.CloudSongResponse> {
          return runCatching {
              val body = HttpManager.request(
                  url = "/api/v1/cloud/get",
                  data = mapOf(
                      "limit" to limit,
                      "offset" to offset
                  ),
                  crypto = HttpManager.CryptoType.WEAPI
              )
              json.decodeFromString(com.rcmiku.ncmapi.model.CloudSongResponse.serializer(), body)
          }
     }
     
     suspend fun userPlaylistV1(userId: Long, trackIds: List<Long>): Result<com.rcmiku.ncmapi.model.UserPlaylistV1Response> {
          return runCatching {
              val body = HttpManager.request(
                  url = "/weapi/user/playlist",
                  data = mapOf(
                      "uid" to userId.toString(),
                      "limit" to "1000",
                      "offset" to "0",
                      "includeVideo" to "true"
                  ),
                  crypto = HttpManager.CryptoType.WEAPI
              )
              val raw = json.decodeFromString(UserPlaylistResponse.serializer(), body)

              val owned = raw.playlist.filter { it.userId == userId }.map { p ->
                  com.rcmiku.ncmapi.model.PlaylistV1(
                      id = p.id,
                      name = p.name,
                      coverImgUrl = p.cover,
                      trackCount = p.trackCount,
                      playCount = p.playCount?.toLong() ?: 0L,
                      containsTracks = trackIds.isNotEmpty()
                  )
              }
              com.rcmiku.ncmapi.model.UserPlaylistV1Response(playlist = owned, code = raw.code)
          }
     }
     
     suspend fun playlistManipulate(playlistId: Long, songIds: List<Long>, manipulateType: PlayManipulateType = PlayManipulateType.ADD): Result<Any> {
          return runCatching {
              val tracks = songIds.map { id -> mapOf("type" to 3, "id" to id.toString()) }
              val url = when (manipulateType) {
                  PlayManipulateType.ADD -> "/api/playlist/track/add"
                  PlayManipulateType.DEL -> "/api/playlist/track/delete"
              }
              val body = HttpManager.request(
                  url = url,
                  data = mapOf(
                      "id" to playlistId,
                      "tracks" to json.encodeToString(tracks)
                  ),
                  crypto = HttpManager.CryptoType.WEAPI
              )
              body
          }
     }
}

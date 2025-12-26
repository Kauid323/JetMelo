package com.rcmiku.music.data.repository

import com.rcmiku.music.data.model.comment.CommentResult
import com.rcmiku.ncmapi.utils.CryptoUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun getComments(
        id: Long,
        type: Int = 0, // 0: song, 1: mv, 2: playlist, 3: album, 4: radio, 5: video
        pageNo: Int = 1,
        pageSize: Int = 20,
        sortType: Int = 1, // 1: recommendation (99 in api), 2: hot, 3: time
        cursor: Long = 0
    ): Result<CommentResult> {
        val resourceTypeMap = mapOf(
            0 to "R_SO_4_",
            1 to "R_MV_5_",
            2 to "A_PL_0_",
            3 to "R_AL_3_",
            4 to "A_DJ_1_",
            5 to "R_VI_62_"
        )

        val threadId = "${resourceTypeMap[type]}$id"
        
        // Mapping sortType to API values
        // 1 -> 99 (Recommendation)
        // 2 -> 2 (Hot)
        // 3 -> 3 (Time)
        var apiSortType = if (sortType == 1) 99 else sortType
        
        var apiCursor = when(apiSortType) {
            99 -> (pageNo - 1) * pageSize
            2 -> "normalHot#" + (pageNo - 1) * pageSize
            3 -> cursor
            else -> cursor
        }

        val payload = buildJsonObject {
            put("threadId", threadId)
            put("pageNo", pageNo)
            put("showInner", true)
            put("pageSize", pageSize)
            put("cursor", apiCursor.toString())
            put("sortType", apiSortType)
            put("csrf_token", "")
        }

        val encrypted = CryptoUtils.weapi(payload.toString())

        return runCatching {
            httpClient.post("/weapi/v2/resource/comments") {
                setBody(FormDataContent(Parameters.build {
                    append("params", encrypted["params"] ?: "")
                    append("encSecKey", encrypted["encSecKey"] ?: "")
                }))
            }.body()
        }
    }

    suspend fun getFloorComments(
        parentCommentId: Long,
        id: Long,
        type: Int = 0,
        limit: Int = 20,
        time: Long = 0
    ): Result<CommentResult> {
        val resourceTypeMap = mapOf(
            0 to "R_SO_4_",
            1 to "R_MV_5_",
            2 to "A_PL_0_",
            3 to "R_AL_3_",
            4 to "A_DJ_1_",
            5 to "R_VI_62_"
        )
        
        val threadId = "${resourceTypeMap[type]}$id"
        
        val payload = buildJsonObject {
            put("parentCommentId", parentCommentId)
            put("threadId", threadId)
            put("time", time)
            put("limit", limit)
        }

        val encrypted = CryptoUtils.weapi(payload.toString())

        return runCatching {
            httpClient.post("/weapi/resource/comment/floor/get") {
                setBody(FormDataContent(Parameters.build {
                    append("params", encrypted["params"] ?: "")
                    append("encSecKey", encrypted["encSecKey"] ?: "")
                }))
            }.body()
        }
    }

    suspend fun likeComment(
        id: Long,
        commentId: Long,
        t: Int, // 1: like, 0: cancel
        type: Int = 0
    ): Result<Any> { // Response type might need adjustment
         val resourceTypeMap = mapOf(
            0 to "R_SO_4_",
            1 to "R_MV_5_",
            2 to "A_PL_0_",
            3 to "R_AL_3_",
            4 to "A_DJ_1_",
            5 to "R_VI_62_"
        )
        val threadId = "${resourceTypeMap[type]}$id"
        val action = if (t == 1) "like" else "unlike"
        
        val payload = buildJsonObject {
            put("threadId", threadId)
            put("commentId", commentId)
        }
        
        val encrypted = CryptoUtils.weapi(payload.toString())

        return runCatching {
            httpClient.post("/weapi/v1/comment/$action") {
                 setBody(FormDataContent(Parameters.build {
                    append("params", encrypted["params"] ?: "")
                    append("encSecKey", encrypted["encSecKey"] ?: "")
                }))
            }.body()
        }
    }
}

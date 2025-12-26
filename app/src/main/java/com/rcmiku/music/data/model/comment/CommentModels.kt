package com.rcmiku.music.data.model.comment

import kotlinx.serialization.Serializable

@Serializable
data class CommentResult(
    val code: Int,
    val data: CommentData? = null
)

@Serializable
data class CommentData(
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val cursor: Long = 0,
    val comments: List<Comment> = emptyList()
)

@Serializable
data class Comment(
    val user: CommentUser,
    val commentId: Long,
    val content: String,
    val time: Long,
    val likedCount: Int = 0,
    val liked: Boolean = false,
    val replyCount: Int = 0,
    val beReplied: List<BeReplied>? = null,
    val ipLocation: IpLocation? = null,
    val parentCommentId: Long = 0
)

@Serializable
data class CommentUser(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String
)

@Serializable
data class BeReplied(
    val user: CommentUser,
    val content: String?,
    val beRepliedCommentId: Long
)

@Serializable
data class IpLocation(
    val location: String? = null
)

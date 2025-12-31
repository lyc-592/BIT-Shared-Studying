package com.example.bitshared.data.model

// 附件模型
data class AttachmentDto(
    val id: Long,
    val originalName: String,
    val accessUrl: String,
    val fileType: String,
    val fileSize: Long,
    val topicId: Long,
    val createdAt: String
)

// 话题详情模型
data class TopicDto(
    val id: Long,
    val title: String,
    val content: String,
    val forumNo: Long,
    val referencePath: String?,
    val author: ProfileDto, // 复用之前的用户信息模型
    val attachments: List<AttachmentDto>? = emptyList(),
    val viewCount: Int = 0,
    val replyCount: Int = 0,
    val likeCount: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

// 话题列表分页
data class TopicPageResponse(
    val content: List<TopicDto>,
    val totalElements: Long,
    val last: Boolean
)

// 论坛概览
data class ForumDto(
    val forumNo: Long,
    val courseName: String,
    val topicCount: Int
)

// 1. 评论基础模型
data class CommentDto(
    val id: Long,
    val topicId: Long,
    val content: String,
    val parentId: Long?,
    val level: Int,
    val likeCount: Int,
    val replyCount: Int,
    val status: Int,
    val createdAt: String,
    val updatedAt: String,
    val author: ProfileDto,
    val targetUserId: Long?,
    val targetUsername: String?,
    val attachments: List<AttachmentDto>? = emptyList()
)

// 2. 评论详情（包含预览回复）
data class CommentDetailDto(
    val comment: CommentDto,
    val replyCount: Int,
    val previewReplies: List<CommentDto>,
    val replyTotalElements: Long
)

// 3. 评论分页模型
data class CommentPageResponse(
    val content: List<CommentDto>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val last: Boolean
)
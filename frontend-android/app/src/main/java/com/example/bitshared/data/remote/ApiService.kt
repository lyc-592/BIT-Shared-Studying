package com.example.bitshared.data.remote

import com.example.bitshared.data.model.ApiResponse
import com.example.bitshared.data.model.CommentDetailDto
import com.example.bitshared.data.model.CommentDto
import com.example.bitshared.data.model.CommentPageResponse
import com.example.bitshared.data.model.CourseDto
import com.example.bitshared.data.model.MajorDto
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.example.bitshared.data.model.FileNode
import com.example.bitshared.data.model.ForumDto
import com.example.bitshared.data.model.GrantRequest
import com.example.bitshared.data.model.LoginRequest
import com.example.bitshared.data.model.PermissionResult
import com.example.bitshared.data.model.ProfileDto
import com.example.bitshared.data.model.ProfileRequest
import com.example.bitshared.data.model.RegisterRequest
import com.example.bitshared.data.model.RevokeRequest
import com.example.bitshared.data.model.TopicDto
import com.example.bitshared.data.model.TopicPageResponse
import com.example.bitshared.data.model.UserDto

interface ApiService {
    @GET("api/majors")
    suspend fun getAllMajors(): ApiResponse<List<MajorDto>>

    @GET("api/majors/{majorNo}/courses")
    suspend fun getCoursesByMajor(@Path("majorNo") majorNo: Long): ApiResponse<List<CourseDto>>

    @GET("api/course/{courseNo}/file-tree")
    suspend fun getFileTree(@Path("courseNo") courseNo: Long): List<FileNode>

    // 1. 新建文件夹：返回 ApiResponse<String>
    @FormUrlEncoded
    @POST("api/files/create_dir")
    suspend fun createDirectory(@Field("dir") dir: String): ApiResponse<String>

    // 2. 删除：对齐
    @FormUrlEncoded
    @POST("api/files/delete")
    suspend fun deleteNode(@Field("dir") dir: String): ApiResponse<String>

    // 3. 上传：对齐
    @Multipart
    @POST("api/files/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("targetDir") targetDir: RequestBody
    ): ApiResponse<String>

    // 4. 下载：保持 Streaming
    @Streaming
    @GET("api/files/download")
    suspend fun downloadFile(@Query("path") path: String): retrofit2.Response<okhttp3.ResponseBody>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<UserDto>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<UserDto>

    // --- Profile ---
    @POST("api/profile")
    suspend fun createProfile(@Body request: ProfileRequest): ProfileDto

    @PUT("api/profile/{userId}")
    suspend fun updateProfile(@Path("userId") userId: Long, @Body request: ProfileRequest): ProfileDto

    @GET("api/profile/{userId}")
    suspend fun getProfile(@Path("userId") userId: Long): ProfileDto

    @GET("api/permissions/check")
    suspend fun checkPermission(
        @Query("userId") userId: Long,
        @Query("courseNo") courseNo: Long
    ): ApiResponse<PermissionResult>

    // 2. 授权
    @POST("api/permissions/grant")
    suspend fun grantPermission(@Body request: GrantRequest): ApiResponse<String>

    // 3. 撤销
    @POST("api/permissions/revoke")
    suspend fun revokePermission(@Body request: RevokeRequest): ApiResponse<String>


    // 获取论坛信息
    @GET("api/forums/by-course/{courseNo}")
    suspend fun getForumInfo(@Path("courseNo") courseNo: Long): ApiResponse<ForumDto>

    // 获取话题列表
    @GET("api/topics/by-forum/{forumId}")
    suspend fun getTopicList(@Path("forumId") forumId: Long): ApiResponse<TopicPageResponse>

    // 获取话题详情
    @GET("api/topics/by-topic/{topicId}")
    suspend fun getTopicDetail(@Path("topicId") topicId: Long): ApiResponse<TopicDto>

    // 创建话题 (混合请求: JSON字段 + 文件)
    @Multipart
    @POST("api/topics/create/{userId}")
    suspend fun createTopic(
        @Path("userId") userId: Long,
        @Part("forumNo") forumNo: RequestBody,
        @Part("title") title: RequestBody,
        @Part("content") content: RequestBody,
        @Part("referencePath") referencePath: RequestBody?,
        @Part attachments: List<MultipartBody.Part>?
    ): ApiResponse<TopicDto>

    @DELETE("api/topics/delete/{topicId}")
    suspend fun deleteTopic(
        @Path("topicId") topicId: Long,
        @Query("userId") userId: Long
    ): ApiResponse<Unit>

    @Streaming
    @GET("api/attachments/download/{forumNo}/{filename}")
    suspend fun downloadAttachmentFile(
        @Path("forumNo") forumNo: Long,
        @Path("filename") filename: String,
        @Query("download") download: Boolean = true
    ): retrofit2.Response<okhttp3.ResponseBody>

    // 获取话题的一级评论
    @GET("api/comments/root/{topicId}")
    suspend fun getRootComments(
        @Path("topicId") topicId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<CommentPageResponse>

    // 创建评论 (支持附件)
    @Multipart
    @POST("api/comments/create/{userId}")
    suspend fun createComment(
        @Path("userId") userId: Long,
        @Part("topicId") topicId: RequestBody,
        @Part("content") content: RequestBody,
        @Part("parentId") parentId: RequestBody?, // 如果是回复则传，否则null
        @Part attachments: List<MultipartBody.Part>?
    ): ApiResponse<CommentDto>

    // 点赞/取消点赞
    @POST("api/comments/like/{commentId}")
    suspend fun likeComment(@Path("commentId") commentId: Long): ApiResponse<Unit>

    @POST("api/comments/unlike/{commentId}")
    suspend fun unlikeComment(@Path("commentId") commentId: Long): ApiResponse<Unit>

    @DELETE("api/comments/delete/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: Long,
        @Query("userId") userId: Long
    ): ApiResponse<Unit>

    // 获取评论详情（包含统计和预览回复）
    @GET("api/comments/detail/{commentId}")
    suspend fun getCommentDetail(@Path("commentId") commentId: Long): ApiResponse<CommentDetailDto>

    @GET("api/comments/{rootId}/replies")
    suspend fun getReplies(
        @Path("rootId") rootId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<CommentPageResponse>
}
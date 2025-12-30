package com.example.bitshared.ui.course

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitshared.data.UserManager
import com.example.bitshared.data.model.*
import com.example.bitshared.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(
    val apiService: ApiService,
    private val userManager: UserManager
) : ViewModel() {

    // --- 论坛/话题状态 ---
    var forumInfo by mutableStateOf<ForumDto?>(null)
    var topics = mutableStateListOf<TopicDto>()
    var isLoading by mutableStateOf(false)

    // --- 评论列表状态 ---
    var rootComments = mutableStateListOf<CommentDto>()
    var isCommentsLoading by mutableStateOf(false)

    // --- 二级回复状态 ---
    var currentReplies = mutableStateListOf<CommentDto>()
    var isRepliesLoading by mutableStateOf(false)

    // --- 交互控制变量 ---
    val currentUserId = userManager.getUserId()
    var selectedParentId by mutableStateOf<Long?>(null)
    var targetName by mutableStateOf<String?>(null)
    var previewImageUrl by mutableStateOf<String?>(null)

    // --- 初始化论坛 ---
    fun initForum(courseNo: Long) {
        viewModelScope.launch {
            isLoading = true
            try {
                val infoRes = apiService.getForumInfo(courseNo)
                if (infoRes.success) forumInfo = infoRes.data
                loadTopics(courseNo)
            } catch (e: Exception) {
                Log.e("BIT_SHARED", "Init Forum Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // --- 加载话题列表 ---
    fun loadTopics(courseNo: Long) {
        viewModelScope.launch {
            try {
                val res = apiService.getTopicList(courseNo)
                if (res.success) {
                    topics.clear()
                    topics.addAll(res.data?.content ?: emptyList())
                }
            } catch (e: Exception) {
                Log.e("BIT_SHARED", "Load Topics Error: ${e.message}")
            }
        }
    }

    // --- 加载一级评论 ---
    fun loadRootComments(topicId: Long) {
        viewModelScope.launch {
            isCommentsLoading = true
            try {
                val res = apiService.getRootComments(topicId)
                if (res.success) {
                    rootComments.clear()
                    rootComments.addAll(res.data?.content ?: emptyList())
                }
            } catch (e: Exception) {
                Log.e("BIT_SHARED", "Load Root Comments Error: ${e.message}")
            } finally {
                isCommentsLoading = false
            }
        }
    }

    // --- 加载二级回复 ---
    fun loadReplies(rootId: Long) {
        viewModelScope.launch {
            isRepliesLoading = true
            try {
                val res = apiService.getReplies(rootId)
                if (res.success) {
                    currentReplies.clear()
                    currentReplies.addAll(res.data?.content ?: emptyList())
                }
            } catch (e: Exception) {
                Log.e("BIT_SHARED", "Load Replies Error: ${e.message}")
            } finally {
                isRepliesLoading = false
            }
        }
    }

    // --- 发布评论/回复（多文件+正确后缀+自动刷新） ---
    fun postComment(
        context: Context,
        topicId: Long,
        content: String,
        parentId: Long? = null,
        rootId: Long? = null,
        fileUris: List<Uri>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                // 1. 转换基础字段
                val topicIdPart = topicId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val contentPart = content.toRequestBody("text/plain".toMediaTypeOrNull())
                val parentIdPart = parentId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

                // 2. 处理附件
                val attachmentParts = fileUris.mapNotNull { uri ->
                    val resolver = context.contentResolver
                    val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                    val fileName = "att_${System.currentTimeMillis()}.${extension ?: "dat"}"

                    val inputStream = resolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        val requestFile = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("attachments", fileName, requestFile)
                    } else null
                }

                // 3. 提交请求
                val res = apiService.createComment(currentUserId, topicIdPart, contentPart, parentIdPart, attachmentParts)
                if (res.success) {
                    loadRootComments(topicId) // 刷新全页评论数
                    if (rootId != null) loadReplies(rootId) // 如果在详情页发，刷新详情页
                    onSuccess()
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "失败: ${res.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BIT_SHARED", "Post Comment Exception: ${e.message}")
            }
        }
    }

    // --- 删除话题 ---
    fun deleteTopic(topicId: Long, courseNo: Long) {
        viewModelScope.launch {
            try {
                val res = apiService.deleteTopic(topicId, currentUserId)
                if (res.success) loadTopics(courseNo)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- 删除评论 ---
    fun removeComment(commentId: Long, topicId: Long, rootId: Long? = null) {
        viewModelScope.launch {
            try {
                val res = apiService.deleteComment(commentId, currentUserId)
                if (res.success) {
                    loadRootComments(topicId)
                    if (rootId != null) loadReplies(rootId)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- 点赞 ---
    fun toggleCommentLike(commentId: Long, currentlyLiked: Boolean, topicId: Long) {
        viewModelScope.launch {
            try {
                if (currentlyLiked) apiService.unlikeComment(commentId)
                else apiService.likeComment(commentId)
                loadRootComments(topicId)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- 附件下载（自动使用原始文件名） ---
    fun downloadAttachment(context: Context, attachment: AttachmentDto, forumNo: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileNameInUrl = attachment.accessUrl.substringAfterLast("/")
                val response = apiService.downloadAttachmentFile(forumNo, fileNameInUrl, true)

                if (response.isSuccessful) {
                    val body = response.body() ?: return@launch
                    val file = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        attachment.originalName
                    )
                    body.byteStream().use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "已保存: ${attachment.originalName}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "下载失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- 发布话题（支持附件） ---
    fun createTopic(
        context: Context,
        courseNo: Long,
        title: String,
        content: String,
        refPath: String?,
        fileUris: List<Uri>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val forumNoPart = courseNo.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val titlePart = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val contentPart = content.toRequestBody("text/plain".toMediaTypeOrNull())
                val refPathPart = refPath?.toRequestBody("text/plain".toMediaTypeOrNull())

                val attachmentParts = fileUris.mapNotNull { uri ->
                    val resolver = context.contentResolver
                    val fileName = resolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        cursor.getString(nameIndex)
                    } ?: "file_${System.currentTimeMillis()}"

                    val inputStream = resolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null) {
                        val requestFile = bytes.toRequestBody((resolver.getType(uri) ?: "*/*").toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("attachments", fileName, requestFile)
                    } else null
                }

                val res = apiService.createTopic(currentUserId, forumNoPart, titlePart, contentPart, refPathPart, attachmentParts)
                if (res.success) onSuccess() else onFailure(res.message ?: "发布失败")
            } catch (e: Exception) {
                onFailure("网络错误: ${e.localizedMessage}")
            }
        }
    }
}
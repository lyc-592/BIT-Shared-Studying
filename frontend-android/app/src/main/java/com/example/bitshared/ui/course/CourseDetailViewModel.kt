package com.example.bitshared.ui.course

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitshared.data.UserManager
import com.example.bitshared.data.model.FileNode
import com.example.bitshared.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val userManager: UserManager,
    private val apiService: ApiService
) : ViewModel() {

    private val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    val fileTree = _fileTree.asStateFlow()

    // 权限状态：控制 UI 是否显示编辑按钮
    var canEdit by mutableStateOf(false)

    var selectedTabIndex by mutableStateOf(0)

    /**
     * 初始化：加载树形图 + 判定权限
     */
    fun init(courseNo: Long) {
        checkPermission(courseNo)
        fetchTree(courseNo)
    }

    private fun checkPermission(courseNo: Long) {
        val role = userManager.getUserRole()
        val userId = userManager.getUserId()

        when {
            role >= 3 -> {
                // 超管/系管：直接拥有权限
                canEdit = true
            }
            role == 1 -> {
                // 普通用户：直接无权
                canEdit = false
            }
            role == 2 -> {
                // 专业管理员：需要去后端查是否管理该特定课程
                viewModelScope.launch {
                    try {
                        val res = apiService.checkPermission(userId, courseNo)
                        if (res.success) {
                            canEdit = res.data?.hasPermission ?: false
                        }
                    } catch (e: Exception) {
                        canEdit = false
                    }
                }
            }
        }
    }

    fun fetchTree(courseNo: Long) {
        viewModelScope.launch {
            try {
                val rawResponse = apiService.getFileTree(courseNo)
                // 递归修正路径
                _fileTree.value = rawResponse.map { fixPaths(it, "") }
            } catch (e: Exception) {
                _fileTree.value = emptyList()
            }
        }
    }

    private fun fixPaths(node: FileNode, parentPath: String): FileNode {
        val currentPath = if (parentPath.isEmpty()) node.name else "$parentPath/${node.name}"
        return node.copy(
            path = currentPath,
            children = node.children?.map { fixPaths(it, currentPath) }
        )
    }
    // 处理新建文件夹
    fun createFolder(parentPath: String, folderName: String, courseNo: Long) {
        viewModelScope.launch {
            try {
                // 路径拼接逻辑：parentPath 已经是 fixPaths 处理过的完整路径
                val finalPath = if (parentPath.isEmpty()) folderName else "$parentPath/$folderName"

                val response = apiService.createDirectory(finalPath)
                if (response.success) {
                    fetchTree(courseNo) // 立即刷新
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 解析失败但可能已成功，强制刷新作为保底
                fetchTree(courseNo)
            }
        }
    }

    // 处理删除
    fun deleteNode(path: String, courseNo: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteNode(path)
                if (response.success) {
                    android.util.Log.d("BIT_SHARED", "后端删除成功，开始刷新列表")

                    // 方案 A：立即从本地内存中删除（用户感觉最快）
                    _fileTree.value = removeNodeFromList(_fileTree.value, path)

                    // 方案 B：重新从后端拉取最新树（确保数据绝对同步）
                    fetchTree(courseNo)
                } else {
                    android.util.Log.e("BIT_SHARED", "后端拒绝删除: ${response.message}")
                }
            } catch (e: Exception) {
                // 如果走到这里，说明解析还是崩了或者网络断了
                android.util.Log.e("BIT_SHARED", "捕获到异常: ${e.message}")
                e.printStackTrace()

                // 容错处理：即便解析崩了，如果日志显示其实删成功了，我们也可以强行刷新一遍
                fetchTree(courseNo)
            }
        }
    }

    /**
     * 辅助函数：递归从树中移除指定路径的节点
     */
    private fun removeNodeFromList(list: List<FileNode>, pathToRemove: String): List<FileNode> {
        return list.filter { it.path != pathToRemove }
            .map { it.copy(children = it.children?.let { child -> removeNodeFromList(child, pathToRemove) }) }
    }
    //下载
    fun downloadFile(context: android.content.Context, path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.downloadFile(path)
                if (response.isSuccessful) {
                    val body = response.body() ?: return@launch
                    val fileName = path.substringAfterLast("/")

                    // 使用系统 Downloads 文件夹
                    val file = java.io.File(
                        android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS
                        ),
                        fileName
                    )

                    body.byteStream().use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "下载完成: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 在 CourseDetailViewModel 中添加
    fun uploadFile(context: android.content.Context, uri: Uri, targetDir: String, courseNo: Long) {
        viewModelScope.launch {
            try {
                val resolver = context.contentResolver
                val fileName = getFileName(resolver, uri)
                val inputStream = resolver.openInputStream(uri) ?: return@launch
                val bytes = inputStream.readBytes()

                // 创建文件部分
                val requestFile = RequestBody.create(
                    (resolver.getType(uri) ?: "application/octet-stream").toMediaTypeOrNull(),
                    bytes
                )
                val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)

                // 创建路径部分 (注意：这里用 plain text)
                val dirBody = RequestBody.create("text/plain".toMediaTypeOrNull(), targetDir)

                val response = apiService.uploadFile(filePart, dirBody)
                if (response.success) {
                    fetchTree(courseNo) // 立即刷新
                    Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                fetchTree(courseNo) // 保底刷新
            }
        }
    }


    private fun getFileName(resolver: android.content.ContentResolver, uri: Uri): String {
        var name = "uploaded_file"
        val cursor = resolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) name = it.getString(nameIndex)
            }
        }
        return name
    }
}

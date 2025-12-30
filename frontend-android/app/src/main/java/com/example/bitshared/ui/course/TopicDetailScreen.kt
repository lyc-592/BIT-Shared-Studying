package com.example.bitshared.ui.course

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.bitshared.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    topicId: Long,
    onBack: () -> Unit,
    viewModel: ForumViewModel = hiltViewModel()
) {
    var topic by remember { mutableStateOf<TopicDto?>(null) }
    val context = LocalContext.current

    var showCommentDialog by remember { mutableStateOf(false) }
    var showReplySheet by remember { mutableStateOf(false) }
    var activeRootComment by remember { mutableStateOf<CommentDto?>(null) }

    LaunchedEffect(topicId) {
        val res = viewModel.apiService.getTopicDetail(topicId)
        if (res.success) topic = res.data
        viewModel.loadRootComments(topicId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("话题详情", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedCard(
                        onClick = {
                            viewModel.selectedParentId = null
                            viewModel.targetName = null
                            showCommentDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            "发表你的评论...",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        topic?.let { t ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                item { TopicContentSection(t, context, viewModel) }
                item {
                    Text(
                        text = "全部评论 (${viewModel.rootComments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(viewModel.rootComments, key = { it.id }) { comment ->
                    CommentItem(
                        comment = comment,
                        currentUserId = viewModel.currentUserId,
                        onLike = { viewModel.toggleCommentLike(comment.id, false, topicId) },
                        onDelete = { viewModel.removeComment(comment.id, topicId) },
                        onReply = {
                            activeRootComment = comment
                            viewModel.selectedParentId = comment.id
                            viewModel.targetName = comment.author.nickname
                            viewModel.loadReplies(comment.id)
                            showReplySheet = true
                        },
                        onDownloadAttachment = { att -> viewModel.downloadAttachment(context, att, t.forumNo) },
                        onPreviewImage = { url -> viewModel.previewImageUrl = url }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    if (showReplySheet && activeRootComment != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showReplySheet = false
                viewModel.targetName = null
            },
            sheetState = sheetState
            // 删除了报错的 windowInsets 参数，改用底部的 imePadding 处理
        ) {
            Column(modifier = Modifier.fillMaxHeight(0.85f).fillMaxWidth().imePadding()) {
                Text(
                    text = "回复详情",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    item {
                        CommentItem(
                            comment = activeRootComment!!,
                            currentUserId = viewModel.currentUserId,
                            onLike = {}, onDelete = {},
                            onReply = {
                                viewModel.selectedParentId = activeRootComment!!.id
                                viewModel.targetName = activeRootComment!!.author.nickname
                                showCommentDialog = true
                            },
                            onDownloadAttachment = { att -> viewModel.downloadAttachment(context, att, topic!!.forumNo) },
                            onPreviewImage = { url -> viewModel.previewImageUrl = url }
                        )
                        HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    items(viewModel.currentReplies, key = { it.id }) { reply ->
                        ReplyItem(reply, viewModel.currentUserId) {
                            viewModel.selectedParentId = reply.id
                            viewModel.targetName = reply.author.nickname
                            showCommentDialog = true
                        }
                    }
                }
                Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            viewModel.selectedParentId = activeRootComment!!.id
                            viewModel.targetName = activeRootComment!!.author.nickname
                            showCommentDialog = true
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Text("回复 @${viewModel.targetName ?: activeRootComment!!.author.nickname}")
                    }
                }
            }
        }
    }

    if (showCommentDialog) {
        PostCommentDialog(
            title = if (viewModel.targetName != null) "回复 @${viewModel.targetName}" else "发表评论",
            onDismiss = { showCommentDialog = false },
            onConfirm = { content, files ->
                viewModel.postComment(
                    context = context,
                    topicId = topicId,
                    content = content,
                    parentId = viewModel.selectedParentId,
                    rootId = activeRootComment?.id,
                    fileUris = files,
                    onSuccess = { showCommentDialog = false }
                )
            }
        )
    }

    if (viewModel.previewImageUrl != null) {
        Dialog(
            onDismissRequest = { viewModel.previewImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { viewModel.previewImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = viewModel.previewImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

// --- 辅助组件 ---

@Composable
fun AttachmentItem(
    att: AttachmentDto,
    forumNo: Long,
    onDownload: (AttachmentDto, Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(att.originalName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${att.fileSize / 1024} KB", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            IconButton(onClick = { onDownload(att, forumNo) }) {
                Icon(Icons.Default.FileDownload, "下载", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: CommentDto,
    currentUserId: Long,
    onLike: () -> Unit,
    onDelete: () -> Unit,
    onReply: () -> Unit,
    onDownloadAttachment: (AttachmentDto) -> Unit,
    onPreviewImage: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountCircle, null, Modifier.size(32.dp), tint = Color.LightGray)
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(comment.author.nickname ?: comment.author.username, style = MaterialTheme.typography.labelLarge)
                Text(comment.createdAt, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            if (comment.author.userId == currentUserId) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteForever, null, tint = Color.Red.copy(0.6f)) }
            }
            TextButton(onClick = onLike) {
                Icon(Icons.Default.ThumbUp, null, Modifier.size(16.dp))
                Text(" ${comment.likeCount}", fontSize = 12.sp)
            }
        }
        Text(comment.content, Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)

        comment.attachments?.forEach { att ->
            val isImg = att.fileType.startsWith("image")
            Surface(
                modifier = Modifier.padding(vertical = 4.dp).clickable { if (isImg) onPreviewImage(att.accessUrl) else onDownloadAttachment(att) },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isImg) Icons.Default.Image else Icons.Default.AttachFile, null, Modifier.size(14.dp), tint = Color.Gray)
                    Text(att.originalName + if (isImg) " [预览]" else "", Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        TextButton(onClick = onReply) {
            Text(if (comment.replyCount > 0) "${comment.replyCount} 条回复 >" else "回复", fontSize = 12.sp)
        }
    }
}

@Composable
fun ReplyItem(reply: CommentDto, currentUserId: Long, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp)) {
        Icon(Icons.Default.AccountCircle, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reply.author.nickname ?: reply.author.username, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (!reply.targetUsername.isNullOrEmpty()) {
                    Text(" ▸ ", style = MaterialTheme.typography.labelSmall)
                    Text(reply.targetUsername!!, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(reply.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
            Text(reply.createdAt, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Icon(Icons.Default.Reply, null, Modifier.size(16.dp), tint = Color.Gray)
    }
}

@Composable
fun TopicContentSection(topic: TopicDto, context: android.content.Context, viewModel: ForumViewModel) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountCircle, null, Modifier.size(40.dp), tint = Color.Gray)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(topic.author.nickname ?: topic.author.username, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("发布于 ${topic.createdAt}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(topic.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        Text(topic.content, style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp)

        if (!topic.referencePath.isNullOrEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().clickable {
                    Toast.makeText(context, "路径: ${topic.referencePath}", Toast.LENGTH_SHORT).show()
                }
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(" 关联资料: ${topic.referencePath}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        if (!topic.attachments.isNullOrEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("附件列表", style = MaterialTheme.typography.titleSmall)
            topic.attachments!!.forEach { att ->
                Spacer(Modifier.height(8.dp))
                // 修复点：这里调用了 AttachmentItem
                AttachmentItem(att, topic.forumNo) { a, f -> viewModel.downloadAttachment(context, a, f) }
            }
        }
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(thickness = 0.5.dp)
    }
}

@Composable
fun PostCommentDialog(title: String, onDismiss: () -> Unit, onConfirm: (String, List<Uri>) -> Unit) {
    var content by remember { mutableStateOf("") }
    val selectedFiles = remember { mutableStateListOf<Uri>() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {
        selectedFiles.addAll(it)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("友善交流...") }
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { launcher.launch("*/*") }) { Icon(Icons.Default.AttachFile, null) }
                    Text("${selectedFiles.size} 个附件", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    if (selectedFiles.isNotEmpty()) {
                        TextButton(onClick = { selectedFiles.clear() }) { Text("清空", color = Color.Red) }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(content, selectedFiles) }) { Text("发送") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
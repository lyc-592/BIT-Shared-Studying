package com.example.bitshared.ui.course

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // 导入常用的 Filled 图标
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bitshared.data.model.FileNode
import com.example.bitshared.data.model.TopicDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseNo: Long,
    courseName: String,
    onBack: () -> Unit,
    onNavigateToCreateTopic: (Long, String?) -> Unit,
    onNavigateToTopicDetail: (Long) -> Unit,
    viewModel: CourseDetailViewModel = hiltViewModel(),
    forumViewModel: ForumViewModel = hiltViewModel()
) {
    var selectedTabIndex = viewModel.selectedTabIndex
    val tabs = listOf("资料", "论坛")

    BackHandler(enabled = selectedTabIndex != 0) {
        viewModel.selectedTabIndex = 0
    }

    // 进入页面时初始化权限和数据
    LaunchedEffect(courseNo) {
        viewModel.init(courseNo)
        forumViewModel.initForum(courseNo)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(courseName) },
                    navigationIcon = {
                        IconButton(onClick = {
                            // 顶部返回按钮也遵循同样的逻辑
                            if (selectedTabIndex == 1) {
                                viewModel.selectedTabIndex = 0
                            } else {
                                onBack()
                            }
                        }) { Icon(Icons.Default.ArrowBack, null) }
                    }
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { viewModel.selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // 只有在论坛 Tab 才显示发帖 FAB
            if (selectedTabIndex == 1) {
                FloatingActionButton(onClick = { onNavigateToCreateTopic(courseNo, null) }) {
                    Icon(Icons.Default.PostAdd, contentDescription = "发帖")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTabIndex) {
                0 -> FileTreeSection(
                    viewModel = viewModel,
                    courseNo = courseNo,
                    onQuoteInForum = { path ->
                        // 引用资料发帖：跳转到发帖页并带上路径
                        viewModel.selectedTabIndex = 1
                        onNavigateToCreateTopic(courseNo, path)
                    }
                )
                1 -> ForumSection(
                    viewModel = forumViewModel,
                    courseNo = courseNo,
                    onNavigateToTopicDetail = onNavigateToTopicDetail
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTreeSection(
    viewModel: CourseDetailViewModel,
    courseNo: Long,
    onQuoteInForum: (String) -> Unit
) {
    val treeData by viewModel.fileTree.collectAsState()
    val context = LocalContext.current

    // 状态管理
    var selectedNode by remember { mutableStateOf<FileNode?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedNode?.let { node ->
                viewModel.uploadFile(context, it, node.path, courseNo)
                Toast.makeText(context, "正在发起上传...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (treeData.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(items = treeData, key = { it.path }) { rootNode ->
                FileTreeItem(
                    node = rootNode,
                    level = 0,
                    onActionClick = {
                        selectedNode = it
                        showActionMenu = true
                    }
                )
            }
        }
    }

    // 弹窗与 BottomSheet 逻辑 (保持在 Section 内部)
    if (showActionMenu && selectedNode != null) {
        ModalBottomSheet(onDismissRequest = { showActionMenu = false }) {
            Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text(selectedNode!!.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

                if (selectedNode!!.type == "file") {
                    ListItem(
                        headlineContent = { Text("下载文件") },
                        leadingContent = { Icon(Icons.Default.Download, null) },
                        modifier = Modifier.clickable {
                            showActionMenu = false
                            viewModel.downloadFile(context, selectedNode!!.path)
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text("在论坛引用发帖") },
                    leadingContent = { Icon(Icons.Default.Forum, null) },
                    modifier = Modifier.clickable {
                        showActionMenu = false
                        onQuoteInForum(selectedNode!!.path)
                    }
                )

                if (viewModel.canEdit) {
                    Divider(Modifier.padding(vertical = 8.dp))
                    if (selectedNode!!.type == "directory") {
                        ListItem(
                            headlineContent = { Text("上传文件") },
                            leadingContent = { Icon(Icons.Default.UploadFile, null) },
                            modifier = Modifier.clickable {
                                showActionMenu = false
                                filePickerLauncher.launch("*/*")
                            }
                        )
                        ListItem(
                            headlineContent = { Text("新建文件夹") },
                            leadingContent = { Icon(Icons.Default.CreateNewFolder, null) },
                            modifier = Modifier.clickable {
                                showActionMenu = false
                                showCreateFolderDialog = true
                            }
                        )
                    }
                    ListItem(
                        headlineContent = { Text("删除", color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                        modifier = Modifier.clickable {
                            showActionMenu = false
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("新建文件夹") },
            text = { OutlinedTextField(value = newFolderName, onValueChange = { newFolderName = it }, label = { Text("名称") }) },
            confirmButton = {
                Button(onClick = {
                    viewModel.createFolder(selectedNode!!.path, newFolderName, courseNo)
                    showCreateFolderDialog = false
                    newFolderName = ""
                }) { Text("创建") }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定删除 ${selectedNode?.name} 吗？") },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Red), onClick = {
                    viewModel.deleteNode(selectedNode!!.path, courseNo)
                    showDeleteConfirmDialog = false
                }) { Text("删除") }
            }
        )
    }
}

@Composable
fun ForumSection(
    viewModel: ForumViewModel,
    courseNo: Long,
    onNavigateToTopicDetail: (Long) -> Unit
) {
    if (viewModel.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(viewModel.topics) { topic ->
                TopicItem(
                    topic = topic,
                    isAuthor = topic.author.userId == viewModel.currentUserId,
                    onDelete = { viewModel.deleteTopic(topic.id, courseNo) },
                    onClick = { onNavigateToTopicDetail(topic.id) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun TopicItem(topic: TopicDto, isAuthor: Boolean, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, null, tint = Color.Gray)
                Text(topic.author.nickname ?: topic.author.username, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                if (isAuthor) {
                    IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, null, tint = Color.Red) }
                }
            }
            Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(topic.content, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)

            if (!topic.referencePath.isNullOrEmpty()) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.padding(top = 8.dp)) {
                    Text("📎 关联资料: ${topic.referencePath.substringAfterLast("/")}", Modifier.padding(4.dp), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FileTreeItem(node: FileNode, level: Int, onActionClick: (FileNode) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    val isDirectory = node.type == "directory"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { if (isDirectory) isExpanded = !isExpanded }
                .padding(start = (level * 24).dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    !isDirectory -> Icons.Default.InsertDriveFile
                    isExpanded -> Icons.Default.FolderOpen
                    else -> Icons.Default.Folder
                },
                contentDescription = null,
                tint = if (isDirectory) Color(0xFFE6A23C) else Color(0xFF909399)
            )
            Text(node.name, Modifier.weight(1f).padding(start = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = { onActionClick(node) }) {
                Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
            }
        }
        if (isDirectory && isExpanded) {
            node.children?.forEach { child ->
                key(child.path) { FileTreeItem(child, level + 1, onActionClick) }
            }
        }
    }
}

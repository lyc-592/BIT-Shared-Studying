package com.example.bitshared.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.bitshared.navigation.Screen
import com.example.bitshared.ui.course.CourseDetailScreen
import com.example.bitshared.ui.course.ForumViewModel
import com.example.bitshared.ui.course.TopicDetailScreen
import com.example.bitshared.ui.home.HomeScreen
import com.example.bitshared.ui.major.MajorDetailScreen
import com.example.bitshared.ui.profile.AdminPanelScreen
import com.example.bitshared.ui.profile.ProfileScreen // 确保导入了个人主页

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 定义哪些页面需要显示底部导航栏
    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Courses.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(
                        Triple(Screen.Home, "主页", Icons.Default.Home),
                        Triple(Screen.Courses, "课程", Icons.Default.List),
                        Triple(Screen.Profile, "我的", Icons.Default.Person)
                    )
                    items.forEach { (screen, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, null) },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. 三个主入口
            composable(Screen.Home.route) {
                HomeScreen(onNavigateToMajor = { no, name ->
                    navController.navigate(Screen.MajorDetail.createRoute(no, name))
                })
            }
            composable(Screen.Courses.route) {
                Text("所有课程列表（开发中）")
            }
            composable(Screen.Profile.route) {
                ProfileScreen(onNavigateToAdmin = { navController.navigate(Screen.AdminPanel.route) })
            }

            // 2. 专业详情
            composable(Screen.MajorDetail.route, arguments = listOf(
                navArgument("majorNo") { type = NavType.LongType },
                navArgument("majorName") { type = NavType.StringType }
            )) { backStackEntry ->
                val majorNo = backStackEntry.arguments?.getLong("majorNo") ?: 0L
                val majorName = backStackEntry.arguments?.getString("majorName") ?: ""
                MajorDetailScreen(majorNo, majorName, onNavigateToCourse = { cNo, cName ->
                    navController.navigate(Screen.CourseDetail.createRoute(cNo, cName))
                })
            }

            // 3. 课程详情 (包含资料和论坛切换)
            composable(Screen.CourseDetail.route, arguments = listOf(
                navArgument("courseNo") { type = NavType.LongType },
                navArgument("courseName") { type = NavType.StringType }
            )) { backStackEntry ->
                val courseNo = backStackEntry.arguments?.getLong("courseNo") ?: 0L
                val courseName = backStackEntry.arguments?.getString("courseName") ?: ""
                CourseDetailScreen(
                    courseNo = courseNo,
                    courseName = courseName,
                    onBack = { navController.popBackStack() },
                    onNavigateToCreateTopic = { cNo, refPath ->
                        navController.navigate(Screen.CreateTopic.createRoute(cNo, refPath))
                    },
                    onNavigateToTopicDetail = { topicId ->
                        navController.navigate(Screen.TopicDetail.createRoute(topicId))
                    }
                )
            }

            // 4. 管理员面板
            composable(Screen.AdminPanel.route) {
                AdminPanelScreen(onBack = { navController.popBackStack() })
            }

            // 5. 话题详情
            composable(Screen.TopicDetail.route, arguments = listOf(
                navArgument("topicId") { type = NavType.LongType }
            )) { backStackEntry ->
                val topicId = backStackEntry.arguments?.getLong("topicId") ?: 0L
                TopicDetailScreen(topicId = topicId, onBack = { navController.popBackStack() })
            }

            // 6. 发帖页面
            composable(Screen.CreateTopic.route, arguments = listOf(
                navArgument("courseNo") { type = NavType.LongType },
                navArgument("refPath") { type = NavType.StringType; nullable = true }
            )) { backStackEntry ->
                val courseNo = backStackEntry.arguments?.getLong("courseNo") ?: 0L
                val refPath = backStackEntry.arguments?.getString("refPath")
                CreateTopicScreen(courseNo = courseNo, initialReferencePath = refPath, onBack = { navController.popBackStack() })
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTopicScreen(
    courseNo: Long,
    initialReferencePath: String?,
    onBack: () -> Unit,
    viewModel: ForumViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // 选中的文件列表
    val selectedFiles = remember { mutableStateListOf<Uri>() }

    // 多文件选择器
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedFiles.addAll(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialReferencePath == null) "发布新话题" else "发起讨论") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, null) }
                },
                actions = {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp))
                    } else {
                        TextButton(
                            onClick = {
                                if (title.isBlank() || content.isBlank()) {
                                    Toast.makeText(context, "请填写标题和内容", Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }
                                isSubmitting = true
                                viewModel.createTopic(
                                    context = context,
                                    courseNo = courseNo,
                                    title = title,
                                    content = content,
                                    refPath = initialReferencePath,
                                    fileUris = selectedFiles,
                                    onSuccess = {
                                        isSubmitting = false
                                        Toast.makeText(context, "发布成功", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    },
                                    onFailure = {
                                        isSubmitting = false
                                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        ) {
                            Text("发布", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // 1. 引用路径展示
            if (initialReferencePath != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("关联资料: $initialReferencePath", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 2. 标题输入
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            // 3. 正文输入
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("说点什么吧...") },
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )

            Spacer(Modifier.height(20.dp))

            // 4. 附件管理
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("附件列表", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { launcher.launch("*/*") }) {
                    Icon(Icons.Default.AddCircleOutline, null)
                    Spacer(Modifier.width(4.dp))
                    Text("添加文件")
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(selectedFiles) { uri ->
                    FileAttachmentRow(uri = uri, onRemove = { selectedFiles.remove(uri) })
                }
            }
        }
    }
}

@Composable
fun FileAttachmentRow(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    val fileName = remember(uri) {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "未知文件"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.InsertDriveFile, null, tint = Color.Gray)
            Text(
                text = fileName,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                maxLines = 1
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Cancel, null, tint = Color.Red)
            }
        }
    }
}
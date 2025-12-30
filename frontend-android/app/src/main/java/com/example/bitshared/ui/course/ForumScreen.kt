package com.example.bitshared.ui.course

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bitshared.data.model.TopicDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumScreen(
    courseNo: Long,
    onBack: () -> Unit,
    onNavigateToTopicDetail: (Long) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: ForumViewModel = hiltViewModel()
) {
    LaunchedEffect(courseNo) { viewModel.initForum(courseNo) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${viewModel.forumInfo?.courseName ?: "课程"} 论坛") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Default.PostAdd, contentDescription = "发帖")
            }
        }
    ) { padding ->
        if (viewModel.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(padding))
        }
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(viewModel.topics) { topic ->
                TopicCard(
                    topic = topic,
                    onClick = { onNavigateToTopicDetail(topic.id) },
                    onDelete = { viewModel.deleteTopic(topic.id, courseNo) },
                    isAuthor = topic.author.userId == viewModel.currentUserId
                )
            }
        }
    }
}

@Composable
fun TopicCard(topic: TopicDto, onClick: () -> Unit, onDelete: () -> Unit, isAuthor: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 作者信息
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.Gray)
                Text(
                    text = topic.author.nickname ?: topic.author.username,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.weight(1f))
                if (isAuthor) {
                    IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, null, tint = Color.Red) }
                }
            }

            Text(text = topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(text = topic.content, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)

            // 引用资料展示
            if (!topic.referencePath.isNullOrEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, null, Modifier.size(16.dp))
                        Text("关联资料: ${topic.referencePath.substringAfterLast("/")}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 附件简略图示
            if (!topic.attachments.isNullOrEmpty()) {
                Text(
                    text = "📎 含有 ${topic.attachments.size} 个附件",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
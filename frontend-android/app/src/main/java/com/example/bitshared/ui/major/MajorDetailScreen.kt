package com.example.bitshared.ui.major

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MajorDetailScreen(
    majorNo: Long,
    majorName: String,
    onNavigateToCourse: (Long, String) -> Unit,
    viewModel: MajorDetailViewModel = hiltViewModel()
) {
    // 进入页面时获取数据
    LaunchedEffect(majorNo) {
        viewModel.fetchCourses(majorNo)
    }

    val courses by viewModel.courses.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(majorName) }) }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(courses) { course ->
                ListItem(
                    headlineContent = { Text(course.courseName) },
                    supportingContent = { Text("课程编号: ${course.courseNo}") },
                    modifier = Modifier.clickable {
                        onNavigateToCourse(course.courseNo, course.courseName)
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
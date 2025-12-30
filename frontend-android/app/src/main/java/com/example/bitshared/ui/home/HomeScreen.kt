package com.example.bitshared.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bitshared.data.model.MajorDto


// ... imports

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMajor: (Long, String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val filteredMajors by viewModel.filteredMajors.collectAsState()
    var active by remember { mutableStateOf(false) }
    val error by viewModel.errorMessage
    val loading by viewModel.isLoading

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 如果有错误显示错误信息
        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(Modifier.padding(8.dp)) {
                    Text(text = error!!, modifier = Modifier.weight(1f))
                    Button(onClick = { viewModel.fetchMajors() }) { Text("重试") }
                }
            }
        }

        SearchBar(
            query = viewModel.searchQuery.value,
            onQueryChange = { viewModel.searchQuery.value = it },
            onSearch = {
                viewModel.onSearchClick()
                active = true
            },
            active = active,
            onActiveChange = { active = it },
            placeholder = { Text(if (loading) "正在加载数据..." else "输入专业名称") },
            leadingIcon = {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Icon(Icons.Default.Search, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn {
                items(filteredMajors) { major ->
                    ListItem(
                        headlineContent = { Text(major.majorName) },
                        supportingContent = { Text("ID: ${major.majorNo}") },
                        modifier = Modifier.clickable {
                            active = false
                            onNavigateToMajor(major.majorNo, major.majorName)
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

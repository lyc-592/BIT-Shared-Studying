package com.example.bitshared.ui.home

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitshared.data.model.MajorDto
import com.example.bitshared.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    var searchQuery = mutableStateOf("")

    private val _filteredMajors = MutableStateFlow<List<MajorDto>>(emptyList())
    val filteredMajors = _filteredMajors.asStateFlow()

    // 新增：用于在 UI 上显示错误或加载状态
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    private var allMajors: List<MajorDto> = emptyList()

    init {
        fetchMajors()
    }

    fun fetchMajors() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                Log.d("BIT_SHARED", "开始请求后端专业列表...")
                val response = apiService.getAllMajors()
                if (response.success) {
                    allMajors = response.data ?: emptyList()
                    Log.d("BIT_SHARED", "请求成功，获得 ${allMajors.size} 个专业")
                } else {
                    errorMessage.value = "后端返回错误: ${response.message}"
                }
            } catch (e: Exception) {
                Log.e("BIT_SHARED", "网络请求异常: ${e.message}")
                e.printStackTrace()
                errorMessage.value = "网络连接失败: ${e.localizedMessage}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun onSearchClick() {
        if (searchQuery.value.isEmpty()) {
            _filteredMajors.value = emptyList()
        } else {
            val result = allMajors.filter {
                it.majorName.contains(searchQuery.value, ignoreCase = true)
            }
            _filteredMajors.value = result
            Log.d("BIT_SHARED", "本地搜索完成，匹配到 ${result.size} 个结果")
        }
    }
}
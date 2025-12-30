package com.example.bitshared.ui.major

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitshared.data.model.CourseDto
import com.example.bitshared.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MajorDetailViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {
    private val _courses = MutableStateFlow<List<CourseDto>>(emptyList())
    val courses = _courses.asStateFlow()

    fun fetchCourses(majorNo: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.getCoursesByMajor(majorNo)
                if (response.success) {
                    _courses.value = response.data ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
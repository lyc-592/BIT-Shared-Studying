package com.example.bitshared.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitshared.data.UserManager
import com.example.bitshared.data.model.GrantRequest
import com.example.bitshared.data.model.MajorDto
import com.example.bitshared.data.model.RevokeRequest
import com.example.bitshared.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val apiService: ApiService,
    private val userManager: UserManager
) : ViewModel() {
    var searchKeyword by mutableStateOf("")
    var allMajors by mutableStateOf<List<MajorDto>>(emptyList())

    init {
        fetchMajors() // 复用之前获取专业的逻辑
    }

    private fun fetchMajors() {
        viewModelScope.launch {
            val res = apiService.getAllMajors()
            if (res.success) allMajors = res.data ?: emptyList()
        }
    }

    fun grant(targetName: String, role: Int, majorNo: Long?, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val res = apiService.grantPermission(
                GrantRequest(userManager.getUserId(), targetName, role, majorNo)
            )
            onResult(res.message ?: "操作完成")
        }
    }

    fun revoke(targetName: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val res = apiService.revokePermission(
                RevokeRequest(userManager.getUserId(), targetName)
            )
            onResult(res.message ?: "操作完成")
        }
    }
}
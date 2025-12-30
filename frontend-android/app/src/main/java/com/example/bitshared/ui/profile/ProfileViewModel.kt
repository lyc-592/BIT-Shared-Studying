package com.example.bitshared.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitshared.data.UserManager
import com.example.bitshared.data.model.LoginRequest
import com.example.bitshared.data.model.ProfileDto
import com.example.bitshared.data.model.ProfileRequest
import com.example.bitshared.data.model.RegisterRequest
import com.example.bitshared.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val apiService: ApiService,
    val userManager: UserManager
) : ViewModel() {

    var userId by mutableStateOf(userManager.getUserId())
    var profile by mutableStateOf<ProfileDto?>(null)
    var isEditMode by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    init {
        if (userId != -1L) fetchProfile()
    }

    fun fetchProfile() {
        viewModelScope.launch {
            try {
                profile = apiService.getProfile(userId)
            } catch (e: Exception) {
                // 如果获取失败且是 404，说明还没创建 profile，由 UI 处理
                e.printStackTrace()
            }
        }
    }

    // ui/profile/ProfileViewModel.kt
    fun login(user: String, pass: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = apiService.login(LoginRequest(user, pass))
                if (res.success) {
                    val userData = res.data!!
                    userId = userData.id
                    userManager.saveUserId(userData.id)
                    // --- 新增：保存角色 ---
                    userManager.saveUserRole(userData.role)

                    fetchProfile()
                    onResult("登录成功")
                } else onResult(res.message ?: "登录失败")
            } catch (e: Exception) { onResult("网络错误") }
        }
    }
    fun updateProfile(nickname: String, bio: String, major: String) {
        viewModelScope.launch {
            try {
                // 如果 profile 为空则调用 create，否则调用 update
                val request = ProfileRequest(userId, nickname, bio, major)
                val newProfile = if (profile == null) {
                    apiService.createProfile(request)
                } else {
                    apiService.updateProfile(userId, request)
                }
                profile = newProfile
                isEditMode = false
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun logout() {
        userManager.logout()
        userId = -1L
        profile = null
    }


    fun register(user: String, pass: String, mail: String, onResult: (String) -> Unit) {
        if (user.isBlank() || pass.isBlank() || mail.isBlank()) {
            onResult("请填写完整信息")
            return
        }

        viewModelScope.launch {
            try {
                val res = apiService.register(RegisterRequest(user, pass, mail))
                if (res.success) {
                    // 注册成功，自动登录
                    userId = res.data!!.id
                    userManager.saveUserId(userId)
                    userManager.saveUserRole(res.data.role)

                    // 注册成功后，后端通常还没有该用户的 Profile，
                    // fetchProfile 可能会报 404，这是正常的。
                    fetchProfile()

                    onResult("注册成功并已登录")
                } else {
                    // 显示后端返回的错误信息（如：用户名已存在）
                    onResult(res.message ?: "注册失败")
                }
            } catch (e: Exception) {
                android.util.Log.e("BIT_SHARED", "注册异常: ${e.message}")
                onResult("网络错误或服务器异常")
            }
        }
    }
}
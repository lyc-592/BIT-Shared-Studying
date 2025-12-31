package com.example.bitshared.ui.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.room.util.copy
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ProfileScreen(
    onNavigateToAdmin: () -> Unit, // 新增：跳转到管理员面板的参数
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userId = viewModel.userId

    if (userId == -1L) {
        AuthSection(viewModel)
    } else {
        if (viewModel.isEditMode) {
            EditProfileSection(viewModel)
        } else {
            // 将跳转逻辑传下去
            DisplayProfileSection(viewModel, onNavigateToAdmin)
        }
    }
}

// 1. 登录/注册界面
@Composable
fun AuthSection(viewModel: ProfileViewModel) {
    var isLogin by remember { mutableStateOf(true) }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isLogin) "欢迎登录" else "账号注册",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("用户名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 只有注册模式下才显示邮箱输入框
        AnimatedVisibility(visible = !isLogin) {
            Column {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (isLogin) {
                    // 调用登录
                    viewModel.login(user, pass) { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 调用注册
                    viewModel.register(user, pass, email) { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(if (isLogin) "立即登录" else "提交注册")
        }

        TextButton(onClick = {
            isLogin = !isLogin
            // 切换时清空输入框，防止干扰
            user = ""; pass = ""; email = ""
        }) {
            Text(if (isLogin) "没有账号？点击注册" else "已有账号？点击登录")
        }
    }
}

// 2. 资料展示界面
@Composable
fun DisplayProfileSection(viewModel: ProfileViewModel, onNavigateToAdmin: () -> Unit) {
    val p = viewModel.profile
    // 从 Profile 或本地存储获取角色（双重保障）
    val role = p?.role ?: viewModel.userManager.getUserRole()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = p?.nickname ?: "新用户",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.width(8.dp))
                    // --- 角色标签 ---
                    RoleBadge(role)
                }
                Spacer(Modifier.height(8.dp))
                Text("专业: ${p?.major ?: "未设置"}")
                Text("简介: ${p?.bio ?: "暂无内容"}")
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- 管理员专属入口 ---
        if (role >= 3) {
            Button(
                onClick = onNavigateToAdmin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(Icons.Default.AdminPanelSettings, null)
                Spacer(Modifier.width(8.dp))
                Text("进入管理员授权中心")
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(onClick = { viewModel.isEditMode = true }, Modifier.fillMaxWidth()) {
            Text("编辑个人资料")
        }

        OutlinedButton(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("退出登录")
        }
    }
}

// 辅助组件：显示角色文字标签
@Composable
fun RoleBadge(role: Int) {
    val (text, color) = when(role) {
        4 -> "超级管理员" to Color(0xFFFF5252) // 红
        3 -> "系统管理员" to Color(0xFFFF9800) // 橙
        2 -> "专业管理员" to Color(0xFF4CAF50) // 绿
        else -> "普通用户" to Color(0xFF9E9E9E) // 灰
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun Color(x0: Long) {
    TODO("Not yet implemented")
}

// 3. 资料编辑界面
@Composable
fun EditProfileSection(viewModel: ProfileViewModel) {
    var nick by remember { mutableStateOf(viewModel.profile?.nickname ?: "") }
    var major by remember { mutableStateOf(viewModel.profile?.major ?: "") }
    var bio by remember { mutableStateOf(viewModel.profile?.bio ?: "") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("编辑资料", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(nick, { nick = it }, label = { Text("昵称") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(major, { major = it }, label = { Text("专业") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(bio, { bio = it }, label = { Text("简介") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.updateProfile(nick, bio, major) }, Modifier.fillMaxWidth()) { Text("保存修改") }
        TextButton(onClick = { viewModel.isEditMode = false }, Modifier.fillMaxWidth()) { Text("取消") }
    }
}

package com.example.bitshared.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bitshared.ui.admin.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    var targetUser by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(2) } // 默认授权 2 (专业管理员)

    // 下拉菜单状态
    var expanded by remember { mutableStateOf(false) }
    var selectedMajorName by remember { mutableStateOf("请选择专业") }
    var selectedMajorNo by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("权限控制台") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // 1. 输入目标用户名
            OutlinedTextField(
                value = targetUser,
                onValueChange = { targetUser = it },
                label = { Text("目标用户名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // 2. 选择角色
            Text("授予角色等级:", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selectedRole == 2, { selectedRole = 2 })
                    Text("专业管理员(2)")
                }
                Spacer(Modifier.width(16.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selectedRole == 3, { selectedRole = 3 })
                    Text("系统管理员(3)")
                }
            }

            Spacer(Modifier.height(16.dp))

            // 3. 专业选择器（仅当选择角色 2 时显示）
            if (selectedRole == 2) {
                Text("选择所属专业:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                // 下拉菜单核心组件
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedMajorName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        viewModel.allMajors.forEach { major ->
                            DropdownMenuItem(
                                text = { Text(major.majorName) },
                                onClick = {
                                    selectedMajorName = major.majorName
                                    selectedMajorNo = major.majorNo
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // 4. 操作按钮
            Button(
                onClick = {
                    if (targetUser.isBlank()) {
                        Toast.makeText(context, "请输入用户名", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedRole == 2 && selectedMajorNo == null) {
                        Toast.makeText(context, "请选择专业", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    viewModel.grant(targetUser, selectedRole, if (selectedRole == 2) selectedMajorNo else null) {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("执行授权")
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    if (targetUser.isBlank()) {
                        Toast.makeText(context, "请输入用户名", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.revoke(targetUser) {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("撤销所有权限")
            }
        }
    }
}
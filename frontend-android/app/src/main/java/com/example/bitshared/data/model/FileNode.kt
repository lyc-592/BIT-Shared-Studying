package com.example.bitshared.data.model

data class FileNode(
    val name: String,
    val path: String,
    val type: String,
    // 将 children 设置为可空，并给默认值
    val children: List<FileNode>? = emptyList(),
    // transient 表示不参与 GSON 的序列化/反序列化，仅用于本地 UI 状态
    @Transient var isExpanded: Boolean = false
)
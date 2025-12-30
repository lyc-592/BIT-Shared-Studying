package com.example.bitshared.data.model

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)
package com.example.bitshared.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserManager @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveUserId(userId: Long) = prefs.edit().putLong("user_id", userId).apply()
    fun getUserId(): Long = prefs.getLong("user_id", -1L)
    fun saveUserRole(role: Int) = prefs.edit().putInt("user_role", role).apply()
    fun getUserRole(): Int = prefs.getInt("user_role", 1)
    fun logout() = prefs.edit().clear().apply()
}
package com.example.bitshared

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.bitshared.ui.MainScreen
import com.example.bitshared.ui.theme.BitSharedTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // 必须加这个，ViewModel 才能注入
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BitSharedTheme {
                MainScreen()
            }
        }
    }
}
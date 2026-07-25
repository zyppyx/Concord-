package com.example.concordmobile_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.example.concordmobile_android.ui.ConcordApp
import com.example.concordmobile_android.ui.theme.ConcordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ConcordTheme {
                ConcordApp()
            }
        }
    }
}

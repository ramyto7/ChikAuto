package com.example.chikauto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.chikauto.navigation.AppNavigation
import com.example.chikauto.ui.theme.ChikAutoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChikAutoTheme {
                AppNavigation()
            }
        }
    }
}
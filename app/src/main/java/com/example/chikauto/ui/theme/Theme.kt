package com.example.chikauto.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ChikAutoColors = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueDark,
    background = LightBackground,
    surface = CardWhite
)

@Composable
fun ChikAutoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChikAutoColors,
        content = content
    )
}
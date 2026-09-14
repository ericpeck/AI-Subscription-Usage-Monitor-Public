package com.ericmbpeck.ai_sum.watch

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun WatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color.Black,
            onBackground = WatchInks.name,
            primary = WatchInks.emptyButton,
            onPrimary = WatchInks.emptyButtonInk,
        ),
        content = content,
    )
}

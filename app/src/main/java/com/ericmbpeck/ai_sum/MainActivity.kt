package com.ericmbpeck.ai_sum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ericmbpeck.ai_sum.ui.AiSumApp
import com.ericmbpeck.ai_sum.ui.theme.AISUMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AISUMTheme {
                AiSumApp()
            }
        }
    }
}

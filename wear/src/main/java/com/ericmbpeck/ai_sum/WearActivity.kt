package com.ericmbpeck.ai_sum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ericmbpeck.ai_sum.watch.SampleWearSnapshots
import com.ericmbpeck.ai_sum.watch.WatchApp

class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sample = BuildConfig.DEBUG && intent.getBooleanExtra("sample", false)
        setContent {
            WatchApp(seed = if (sample) SampleWearSnapshots.boardClaude() else null)
        }
    }
}

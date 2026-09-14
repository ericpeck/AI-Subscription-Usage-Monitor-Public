package com.ericmbpeck.ai_sum.watch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import com.ericmbpeck.ai_sum.wearbridge.WearSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun WatchApp(seed: WearSnapshot? = null) {
    val context = LocalContext.current
    val store = remember(seed) {
        WearSnapshotStore(context, seed ?: WearSnapshot(updatedEpochMs = 0L))
    }
    DisposableEffect(store) {
        store.start()
        onDispose { store.stop() }
    }
    val snapshot by store.snapshot.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    WatchTheme {
        AppScaffold(
            timeText = {},
            containerColor = Color.Black,
        ) {
            ProvideWatchScale {
                if (snapshot.accounts.isEmpty()) {
                    EmptyWatchScreen(
                        onOpenPhone = {
                            val app = context.applicationContext
                            scope.launch(Dispatchers.IO) {
                                runCatching { OpenOnPhone.launch(app) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                    )
                } else {
                    val pager = rememberPagerState { snapshot.accounts.size }
                    HorizontalPager(
                        state = pager,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                    ) { page ->
                        UsageArcFace(snapshot.accounts[page])
                    }
                }
            }
        }
    }
}

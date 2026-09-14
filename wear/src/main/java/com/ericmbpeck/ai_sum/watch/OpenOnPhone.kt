package com.ericmbpeck.ai_sum.watch

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.ericmbpeck.ai_sum.wearbridge.WearDataPaths
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

object OpenOnPhone {
    /**
     * RemoteActivityHelper only delivers ACTION_VIEW + CATEGORY_BROWSABLE + a
     * data URI. MAIN/LAUNCHER is rejected with IllegalArgumentException, which
     * is why 3g's button did nothing on a physical Galaxy Watch.
     */
    fun remoteIntent(): Intent =
        Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(WearDataPaths.OPEN_URI))

    suspend fun launch(context: Context) {
        val app = context.applicationContext
        val nodes = runCatching {
            Wearable.getNodeClient(app).connectedNodes.await()
        }.getOrDefault(emptyList())
        nodes.forEach { node ->
            runCatching {
                Wearable.getMessageClient(app)
                    .sendMessage(node.id, WearDataPaths.OPEN, ByteArray(0))
                    .await()
            }
        }
        runCatching {
            RemoteActivityHelper(app)
                .startRemoteActivity(remoteIntent())
                .get(8, TimeUnit.SECONDS)
        }
    }
}

package com.ericmbpeck.ai_sum.wear

import android.app.Application
import com.ericmbpeck.ai_sum.data.FakeAccountStore
import com.ericmbpeck.ai_sum.wearbridge.WearDataPaths
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WearSnapshotPublisher {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun start(app: Application) {
        val dataClient = runCatching { Wearable.getDataClient(app) }.getOrNull() ?: return
        val messageClient = runCatching { Wearable.getMessageClient(app) }.getOrNull() ?: return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            FakeAccountStore.accounts.collect { accounts ->
                val snapshot = WearSnapshotMapper.from(accounts)
                val encoded = json.encodeToString(snapshot)
                val put = PutDataMapRequest.create(WearDataPaths.SNAPSHOT)
                put.dataMap.putString(WearDataPaths.JSON_KEY, encoded)
                put.setUrgent()
                runCatching { dataClient.putDataItem(put.asPutDataRequest()) }
            }
        }
        messageClient.addListener { event ->
            if (event.path != WearDataPaths.OPEN) return@addListener
            WearOpenOnPhone.bringToFront(app)
        }
    }
}

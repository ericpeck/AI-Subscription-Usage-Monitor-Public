package com.ericmbpeck.ai_sum.watch

import android.content.Context
import com.ericmbpeck.ai_sum.wearbridge.WearDataPaths
import com.ericmbpeck.ai_sum.wearbridge.WearSnapshot
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class WearSnapshotStore(
    context: Context,
    seed: WearSnapshot = WearSnapshot(updatedEpochMs = 0L),
) : DataClient.OnDataChangedListener {
    private val app = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val dataClient = Wearable.getDataClient(app)
    private val _snapshot = MutableStateFlow(seed)
    val snapshot: StateFlow<WearSnapshot> = _snapshot.asStateFlow()

    fun start() {
        dataClient.addListener(this)
        dataClient.dataItems.addOnSuccessListener { buffer ->
            buffer.forEach { item -> read(item) }
            buffer.release()
        }
    }

    fun stop() {
        dataClient.removeListener(this)
    }

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                read(event.dataItem)
            }
            if (event.type == DataEvent.TYPE_DELETED &&
                event.dataItem.uri.path == WearDataPaths.SNAPSHOT
            ) {
                _snapshot.value = WearSnapshot(updatedEpochMs = 0L)
            }
        }
        events.release()
    }

    private fun read(item: DataItem) {
        if (item.uri.path != WearDataPaths.SNAPSHOT) return
        val raw = DataMapItem.fromDataItem(item).dataMap.getString(WearDataPaths.JSON_KEY)
            ?: return
        runCatching {
            json.decodeFromString(WearSnapshot.serializer(), raw)
        }.onSuccess { parsed ->
            _snapshot.value = parsed
        }
    }
}

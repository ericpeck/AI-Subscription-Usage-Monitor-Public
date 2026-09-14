package com.ericmbpeck.ai_sum.wear

import com.ericmbpeck.ai_sum.wearbridge.WearDataPaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearMessageListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearDataPaths.OPEN) return
        WearOpenOnPhone.bringToFront(this)
    }
}

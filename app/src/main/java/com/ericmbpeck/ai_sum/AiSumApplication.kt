package com.ericmbpeck.ai_sum

import android.app.Application
import com.ericmbpeck.ai_sum.data.FakeAccountStore
import com.ericmbpeck.ai_sum.wear.WearSnapshotPublisher

class AiSumApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FakeAccountStore.attach(filesDir)
        WearSnapshotPublisher.start(this)
    }
}

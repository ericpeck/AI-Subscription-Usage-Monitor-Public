package com.ericmbpeck.ai_sum.wear

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ericmbpeck.ai_sum.MainActivity
import com.ericmbpeck.ai_sum.wearbridge.WearDataPaths

object WearOpenOnPhone {
    fun bringToFront(context: Context) {
        val launch = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(WearDataPaths.OPEN_URI))
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        context.startActivity(launch)
    }
}

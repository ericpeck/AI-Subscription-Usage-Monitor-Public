package com.ericmbpeck.ai_sum.wear

import com.ericmbpeck.ai_sum.model.Account
import com.ericmbpeck.ai_sum.model.Platforms
import com.ericmbpeck.ai_sum.wearbridge.WearAccount
import com.ericmbpeck.ai_sum.wearbridge.WearResetCopy
import com.ericmbpeck.ai_sum.wearbridge.WearSegment
import com.ericmbpeck.ai_sum.wearbridge.WearSnapshot
import com.ericmbpeck.ai_sum.wearbridge.WearWindow
import java.time.Instant
import java.time.ZoneId

object WearSnapshotMapper {
    fun from(
        accounts: List<Account>,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): WearSnapshot {
        val nowMs = now.toEpochMilli()
        return WearSnapshot(
            updatedEpochMs = nowMs,
            accounts = accounts.map { account -> toWearAccount(account, nowMs, zone) },
        )
    }

    private fun toWearAccount(account: Account, nowMs: Long, zone: ZoneId): WearAccount {
        val platform = Platforms.catalog.firstOrNull { it.id == account.platformId }
        return WearAccount(
            id = account.id,
            platformId = account.platformId,
            displayName = platform?.displayName ?: account.platformId,
            kind = account.kind.name,
            planLine = account.planLine,
            viaLine = viaLine(account),
            needsReauth = account.needsReauth,
            windows = account.windows.map { window ->
                WearWindow(
                    name = window.name,
                    usagePercent = window.usagePercent,
                    elapsedPercent = window.elapsedPercent,
                    resetLabel = window.resetLabel,
                    curveWhen = WearResetCopy.curveWhen(
                        resetsAtEpochMs = window.resetsAtEpochMs,
                        nowEpochMs = nowMs,
                        fallbackResetLabel = window.resetLabel,
                        zone = zone,
                    ),
                    remainingMs = window.resetsAtEpochMs?.minus(nowMs),
                    segments = window.segments.map { WearSegment(it.label, it.percent) },
                )
            },
        )
    }

    private fun viaLine(account: Account): String {
        if (account.platformId != Platforms.GROK_BOT) return ""
        return if (account.planLine.contains("Cursor", ignoreCase = true)) {
            "via Cursor"
        } else {
            "via SuperGrok"
        }
    }
}

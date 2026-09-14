package com.ericmbpeck.ai_sum.data

import com.ericmbpeck.ai_sum.model.Account
import com.ericmbpeck.ai_sum.model.AccountKind
import com.ericmbpeck.ai_sum.model.Platforms
import com.ericmbpeck.ai_sum.model.UsageWindow
import com.ericmbpeck.ai_sum.usage.CursorUsageMapper
import com.ericmbpeck.ai_sum.usage.LiveSignIn
import com.ericmbpeck.ai_sum.usage.ResetFormatter
import com.ericmbpeck.ai_sum.usage.UsageReadResult
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object FakeAccountStore {
    internal const val FILE_NAME = "accounts.json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val nextId = AtomicLong(1)
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    private var storeFile: File? = null
    private var lastGrokBotFromCursor: UsageReadResult.GrokBotUsage? = null
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    fun attach(dir: File) {
        storeFile = File(dir, FILE_NAME)
        load()
    }

    fun add(platformId: String, work: Boolean): Account {
        require(accountFor(platformId) == null) { "One account per platform: $platformId" }
        val kind = if (work) AccountKind.WORK else AccountKind.PERSONAL
        val account = fakeAccount(platformId, nextId.getAndIncrement(), kind)
            ?.withBoardIdentity()
            ?: error("No fake data for $platformId")
        publish(_accounts.value + account)
        return account
    }

    fun rememberGrokBotFromCursor(usage: UsageReadResult.GrokBotUsage) {
        lastGrokBotFromCursor = usage
        val existing = accountFor(Platforms.GROK_BOT) ?: return
        updateLive(
            accountId = existing.id,
            planLine = usage.planLine,
            windows = usage.windows,
        )
    }

    fun addGrokBotFromCursor(work: Boolean): Account? {
        val cursor = accountFor(Platforms.CURSOR) ?: return null
        if (!cursor.liveRead || cursor.needsReauth) return null
        val snap = lastGrokBotFromCursor
        return addLive(
            platformId = Platforms.GROK_BOT,
            work = work,
            planLine = snap?.planLine ?: CursorUsageMapper.GROK_BOT_PLAN_VIA_CURSOR,
            footNote = CursorUsageMapper.GROK_BOT_FOOTNOTE,
            windows = snap?.windows ?: CursorUsageMapper.idleGrokBotWindows(),
        )
    }

    fun addLive(
        platformId: String,
        work: Boolean,
        planLine: String,
        footNote: String,
        windows: List<UsageWindow>,
    ): Account {
        require(accountFor(platformId) == null) { "One account per platform: $platformId" }
        val kind = if (work) AccountKind.WORK else AccountKind.PERSONAL
        val account = Account(
            id = nextId.getAndIncrement(),
            platformId = platformId,
            kind = kind,
            planLine = planLine,
            footNote = footNote,
            updatedLabel = ResetFormatter.updatedJustNow(),
            windows = windows,
            liveRead = true,
            needsReauth = false,
            email = "",
            accountDetailLine = "${planLine.substringBefore(" · ").ifBlank { planLine }} · signed in on this phone",
            notifyOnReset = !work,
        )
        publish(_accounts.value + account)
        return account
    }

    fun updateLive(
        accountId: Long,
        planLine: String,
        windows: List<UsageWindow>,
        needsReauth: Boolean = false,
    ) {
        publish(
            _accounts.value.map { account ->
                if (account.id != accountId) {
                    account
                } else {
                    account.copy(
                        planLine = planLine,
                        windows = windows,
                        updatedLabel = ResetFormatter.updatedJustNow(),
                        liveRead = true,
                        needsReauth = needsReauth,
                    )
                }
            },
        )
    }

    fun markNeedsReauth(accountId: Long) {
        publish(
            _accounts.value.map { account ->
                if (account.id != accountId) account else account.copy(needsReauth = true)
            },
        )
    }

    fun markLiveNeedsReauth(platformId: String) {
        publish(
            _accounts.value.map { account ->
                if (account.liveRead && account.platformId == platformId) {
                    account.copy(needsReauth = true)
                } else {
                    account
                }
            },
        )
    }

    fun clearLiveReauth(platformId: String) {
        publish(
            _accounts.value.map { account ->
                if (account.liveRead && account.platformId == platformId) {
                    account.copy(needsReauth = false)
                } else {
                    account
                }
            },
        )
    }

    fun markLiveClaudeNeedsReauth() = markLiveNeedsReauth(Platforms.CLAUDE)

    fun clearLiveClaudeReauth() = clearLiveReauth(Platforms.CLAUDE)

    fun setNotifyOnReset(accountId: Long, enabled: Boolean) {
        publish(
            _accounts.value.map { account ->
                if (account.id != accountId) {
                    account
                } else {
                    account.copy(notifyOnReset = enabled)
                }
            },
        )
    }

    fun remove(accountId: Long): Account? {
        val current = _accounts.value
        val index = current.indexOfFirst { it.id == accountId }
        if (index < 0) return null
        val remaining = current.filter { it.id != accountId }
        publish(remaining)
        if (remaining.isEmpty()) return null
        return remaining[index.coerceAtMost(remaining.lastIndex)]
    }

    fun hasLive(platformId: String): Boolean =
        _accounts.value.any { it.liveRead && it.platformId == platformId }

    fun hasLiveClaude(): Boolean = hasLive(Platforms.CLAUDE)

    fun hasLiveWebSession(): Boolean =
        _accounts.value.any { it.liveRead && LiveSignIn.usesInAppBrowser(it.platformId) }

    fun addCursor(work: Boolean): Account = add(Platforms.CURSOR, work)

    fun account(id: Long): Account? = _accounts.value.firstOrNull { it.id == id }

    fun accountFor(platformId: String): Account? =
        _accounts.value.firstOrNull { it.platformId == platformId }

    internal fun replaceAll(accounts: List<Account>) {
        val maxId = accounts.maxOfOrNull { it.id } ?: 0L
        nextId.set(maxId + 1)
        publish(accounts)
    }

    internal fun detach() {
        storeFile = null
        lastGrokBotFromCursor = null
        nextId.set(1)
        _accounts.value = emptyList()
    }

    private fun publish(accounts: List<Account>) {
        _accounts.value = accounts
        persist()
    }

    private fun load() {
        val file = storeFile ?: return
        if (!file.isFile) return
        val snapshot = runCatching {
            json.decodeFromString(StoredAccounts.serializer(), file.readText())
        }.getOrNull() ?: return
        val kept = snapshot.accounts.filter { account ->
            Platforms.catalog.firstOrNull { it.id == account.platformId }?.addableThisSlice == true
        }
        val maxId = snapshot.accounts.maxOfOrNull { it.id } ?: 0L
        nextId.set(maxOf(snapshot.nextId, maxId + 1))
        _accounts.value = kept
        if (kept.size != snapshot.accounts.size) {
            persist()
        }
    }

    private fun persist() {
        val file = storeFile ?: return
        val snapshot = StoredAccounts(
            nextId = nextId.get(),
            accounts = _accounts.value,
        )
        runCatching {
            val parent = file.parentFile ?: return@runCatching
            if (!parent.exists()) parent.mkdirs()
            val tmp = File(parent, "${file.name}.tmp")
            tmp.writeText(json.encodeToString(StoredAccounts.serializer(), snapshot))
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
        }
    }
}

@Serializable
private data class StoredAccounts(
    val nextId: Long,
    val accounts: List<Account>,
)

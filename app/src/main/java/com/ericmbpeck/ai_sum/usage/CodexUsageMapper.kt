package com.ericmbpeck.ai_sum.usage

import com.ericmbpeck.ai_sum.model.UsageWindow
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object CodexUsageMapper {
    const val SESSION_NAME: String = "Current session"
    const val WEEKLY_NAME: String = "Weekly limit"
    const val FOOTNOTE: String =
        "Local and cloud tasks draw on the same two caps. Upgrading the ChatGPT plan raises both."

    private const val WEEKLY_SECONDS: Long = 6L * 24L * 3600L

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun map(raw: String, now: Instant, zone: ZoneId): UsageReadResult {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }
            .getOrElse { return UsageReadResult.Failed("Could not read usage") }
        return when (root.string("kind")) {
            "needs_sign_in", "wrong_origin" -> UsageReadResult.NeedsSignIn
            "free" -> UsageReadResult.FreePlan
            "error" -> UsageReadResult.Failed(
                root.string("message") ?: "Could not read usage",
            )
            else -> mapOk(root, now, zone)
        }
    }

    private fun mapOk(root: JsonObject, now: Instant, zone: ZoneId): UsageReadResult {
        val planRaw = planName(root)
        val parsed = linkedMapOf<String, Pair<Float, Instant?>>()
        mergeWindows(parsed, windowsFromArray(root["windows"], zone, now))
        mergeWindows(parsed, windowsFromRateLimit(root["rate_limit"] ?: root["rateLimit"], zone, now))
        if (isFreePlan(planRaw) && parsed.isEmpty()) {
            return UsageReadResult.FreePlan
        }
        if (parsed.isEmpty()) {
            return UsageReadResult.Failed("Could not read usage")
        }
        val session = parsed[SESSION_NAME]?.let { (percent, resets) ->
            usageWindow(SESSION_NAME, percent, resets, now, zone, ResetFormatter.FIVE_HOURS)
        } ?: idleWindow(SESSION_NAME, ResetFormatter.FIVE_HOURS, now, zone)
        val weekly = parsed[WEEKLY_NAME]?.let { (percent, resets) ->
            usageWindow(WEEKLY_NAME, percent, resets, now, zone, ResetFormatter.SEVEN_DAYS)
        } ?: idleWindow(WEEKLY_NAME, ResetFormatter.SEVEN_DAYS, now, zone)
        val planLine = if (planRaw.isBlank()) {
            "Signed in · rolling session and weekly caps"
        } else {
            "${displayPlan(planRaw)} · rolling session and weekly caps"
        }
        return UsageReadResult.Success(
            planLine = planLine,
            windows = listOf(session, weekly),
        )
    }

    internal fun isFreePlan(planRaw: String): Boolean {
        val n = planRaw.lowercase().replace('_', ' ').trim()
        if (n.isEmpty()) return false
        if (Regex("plus|pro|business|team|enterprise|edu").containsMatchIn(n)) return false
        return n == "free" || n == "go" || n == "guest" || n.contains("free workspace")
    }

    internal fun displayPlan(raw: String): String {
        val compact = raw.trim().replace('_', ' ')
        if (compact.isEmpty()) return compact
        val n = compact.lowercase()
        return when {
            n == "plus" -> "Plus"
            n == "go" -> "Go"
            n == "pro" || n == "prolite" || n.startsWith("pro ") -> "Pro"
            n.contains("business") -> "Business"
            n.contains("team") -> "Team"
            n.contains("enterprise") || n == "ent26" -> "Enterprise"
            n.contains("edu") -> "Edu"
            else -> compact.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
    }

    internal fun isWeeklyDuration(seconds: Long?): Boolean =
        seconds != null && seconds >= WEEKLY_SECONDS

    private fun mergeWindows(
        into: MutableMap<String, Pair<Float, Instant?>>,
        extra: Map<String, Pair<Float, Instant?>>,
    ) {
        extra.forEach { (name, value) ->
            if (!into.containsKey(name)) into[name] = value
        }
    }

    private fun windowsFromArray(
        element: JsonElement?,
        zone: ZoneId,
        now: Instant,
    ): Map<String, Pair<Float, Instant?>> {
        if (element !is JsonArray) return emptyMap()
        val out = linkedMapOf<String, Pair<Float, Instant?>>()
        element.forEach { item ->
            val obj = item as? JsonObject ?: return@forEach
            val percent = percentOf(obj) ?: return@forEach
            val seconds = intOf(obj, "limit_window_seconds")
                ?: intOf(obj, "window_minutes")?.let { it * 60 }
            val name = displayWindow(obj.string("name") ?: obj.string("label"), seconds)
            val resets = parseReset(
                obj["resets_at"] ?: obj["reset_at"] ?: obj["resetsAt"] ?: obj["resetAt"],
                intOf(obj, "reset_after_seconds"),
                now,
                zone,
            )
            if (!out.containsKey(name)) out[name] = percent to resets
        }
        return out
    }

    private fun windowsFromRateLimit(
        element: JsonElement?,
        zone: ZoneId,
        now: Instant,
    ): Map<String, Pair<Float, Instant?>> {
        val rl = unwrapObject(element) ?: return emptyMap()
        val out = linkedMapOf<String, Pair<Float, Instant?>>()
        listOf("primary_window", "primaryWindow", "secondary_window", "secondaryWindow").forEach { key ->
            val win = unwrapObject(rl[key]) ?: return@forEach
            val percent = percentOf(win) ?: return@forEach
            val seconds = intOf(win, "limit_window_seconds")
            val name = displayWindow(win.string("name"), seconds)
            val resets = parseReset(
                win["reset_at"] ?: win["resets_at"] ?: win["resetAt"],
                intOf(win, "reset_after_seconds"),
                now,
                zone,
            )
            if (!out.containsKey(name)) out[name] = percent to resets
        }
        return out
    }

    internal fun displayWindow(raw: String?, seconds: Long?): String {
        if (isWeeklyDuration(seconds)) return WEEKLY_NAME
        val n = raw.orEmpty().trim().lowercase().replace('_', ' ')
        if (n.contains("week") || n.contains("seven")) return WEEKLY_NAME
        return SESSION_NAME
    }

    private fun percentOf(obj: JsonObject): Float? {
        val keys = listOf("used_percent", "percent", "utilization", "usage_percent", "percent_used")
        for (key in keys) {
            val n = obj[key]?.jsonPrimitive?.floatOrNull ?: continue
            val scaled = if (key == "utilization" && n <= 1f) n * 100f else n
            if (scaled in 0f..100f) return scaled
        }
        return null
    }

    private fun parseReset(
        element: JsonElement?,
        resetAfterSeconds: Long?,
        now: Instant,
        zone: ZoneId,
    ): Instant? {
        val fromElement = instantOf(element)
        if (fromElement != null) return fromElement
        if (resetAfterSeconds != null && resetAfterSeconds > 0L) {
            return now.plusSeconds(resetAfterSeconds)
        }
        return null
    }

    private fun instantOf(element: JsonElement?): Instant? {
        if (element == null || element is JsonNull) return null
        if (element is JsonPrimitive) {
            val asLong = element.longOrNull ?: element.contentOrNull?.toLongOrNull()
            if (asLong != null && asLong > 0L) {
                return if (asLong > 1_000_000_000_000L) {
                    Instant.ofEpochMilli(asLong)
                } else {
                    Instant.ofEpochSecond(asLong)
                }
            }
            val text = element.contentOrNull ?: return null
            runCatching { return Instant.parse(text) }
            return CursorUsageMapper.parseReset(text, zone = ZoneId.of("UTC"))
        }
        return null
    }

    private fun idleWindow(
        name: String,
        length: Duration,
        now: Instant,
        zone: ZoneId,
    ): UsageWindow = usageWindow(name, 0f, now.plus(length), now, zone, length)

    private fun usageWindow(
        name: String,
        percent: Float,
        resetsAt: Instant?,
        now: Instant,
        zone: ZoneId,
        length: Duration,
    ): UsageWindow {
        val elapsed = if (resetsAt != null) {
            ResetFormatter.elapsedPercent(resetsAt, now, length)
        } else {
            0f
        }
        val resetLabel = if (resetsAt != null) {
            ResetFormatter.resetLabel(resetsAt, now, zone)
        } else {
            ResetFormatter.resetLabel(now.plus(length), now, zone)
        }
        return UsageWindow(
            name = name,
            usagePercent = percent.coerceIn(0f, 100f),
            elapsedPercent = elapsed,
            resetLabel = resetLabel,
            elapsedLabel = ResetFormatter.elapsedLabel(elapsed),
            resetsAtEpochMs = (resetsAt ?: now.plus(length)).toEpochMilli(),
        )
    }

    private fun planName(root: JsonObject): String {
        val direct = root.string("plan") ?: root.string("plan_type") ?: root.string("planType")
        if (!direct.isNullOrBlank()) return direct
        return planName(root["subscription"])
    }

    private fun planName(element: JsonElement?): String {
        if (element == null || element is JsonNull) return ""
        if (element is JsonObject) {
            return element.string("plan")
                ?: element.string("plan_type")
                ?: element.string("planType")
                ?: element.string("tier")
                ?: ""
        }
        return runCatching { element.jsonPrimitive.contentOrNull }.getOrNull().orEmpty()
    }

    private fun unwrapObject(element: JsonElement?): JsonObject? = when (element) {
        null, is JsonNull -> null
        is JsonObject -> element
        else -> null
    }

    private fun intOf(obj: JsonObject, key: String): Long? {
        val el = obj[key] ?: return null
        if (el is JsonPrimitive) {
            return el.longOrNull ?: el.floatOrNull?.toLong()
        }
        return null
    }

    private fun JsonObject.string(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()?.takeIf { it.isNotBlank() }
}

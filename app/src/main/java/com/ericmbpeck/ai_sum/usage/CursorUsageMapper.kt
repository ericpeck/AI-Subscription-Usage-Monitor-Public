package com.ericmbpeck.ai_sum.usage

import com.ericmbpeck.ai_sum.model.UsageWindow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.MonthDay
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object CursorUsageMapper {
    const val CURSOR_MODELS: String = "Cursor Models"
    const val OTHER_MODELS: String = "Other Models"
    const val FOOTNOTE: String =
        "Cursor's own models draw on the included allowance; other models bill separately once it runs out."
    const val GROK_BOT_WINDOW: String = "Weekly session"
    const val GROK_BOT_PLAN_VIA_CURSOR: String =
        "Included with Cursor\nBilled with your Cursor subscription"
    const val GROK_BOT_FOOTNOTE: String =
        "Grok Bot rides on whichever plan you hold — SuperGrok or Cursor. Its weekly session is counted on its own, separate from the parent subscription's limits."

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val resetDateTime: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.US)
    private val resetDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
    private val resetMonthDay: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val preferredOrder = listOf(CURSOR_MODELS, OTHER_MODELS)

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
        if (isFreePlan(planRaw)) {
            return UsageReadResult.FreePlan
        }
        val parsed = windows(root["windows"], zone, now)
        val fallbackPercent = percentOf(root, "percent") ?: percentOf(root, "utilization")
        val fallbackReset = parseReset(root.string("resets_at") ?: root.string("resetsAt"), zone, now)
        val usageWindows = if (parsed.isNotEmpty()) {
            parsed
        } else if (fallbackPercent != null) {
            listOf(usageWindow("Included usage", fallbackPercent, fallbackReset, now, zone))
        } else {
            return UsageReadResult.Failed("Could not read usage")
        }
        val planLine = if (planRaw.isBlank()) {
            "Signed in · Cursor and other models counted apart"
        } else {
            "${displayPlan(planRaw)} · Cursor and other models counted apart"
        }
        val grokBot = grokBotUsage(root, now, zone)
        return UsageReadResult.Success(
            planLine = planLine,
            windows = usageWindows,
            grokBot = grokBot,
        )
    }

    internal fun isFreePlan(planRaw: String): Boolean {
        val n = planRaw.lowercase()
        if (n.isEmpty()) return false
        if (Regex("pro|ultra|business|team|enterprise").containsMatchIn(n)) return false
        return Regex("(^|[^a-z])(free|hobby)([^a-z]|$)").containsMatchIn(n)
    }

    internal fun displayPlan(raw: String): String {
        val compact = raw.trim().replace('_', ' ')
        if (compact.isEmpty()) return compact
        val lower = compact.lowercase()
        if (lower == "ultra") return "Ultra"
        if (lower == "pro+" || lower == "pro plus") return "Pro+"
        if (lower == "pro") return "Pro"
        if (lower.contains("business")) return "Business"
        return compact.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }

    /**
     * Spending shows CURRENT PLAN and an Upgrade card on the same page.
     * Never take Ultra from "Upgrade to Ultra" when CURRENT PLAN is Pro+.
     */
    internal fun planFromSpendingText(text: String): String {
        val current = Regex(
            """CURRENT PLAN[\s\S]{0,60}?(Pro\+|Ultra|Pro|Business|Team|Enterprise)""",
            RegexOption.IGNORE_CASE,
        ).find(text)
        if (current != null) return displayPlan(current.groupValues[1])
        val stripped = Regex(
            """UPGRADE AVAILABLE[\s\S]{0,240}|upgrade\s+to\s+ultra""",
            RegexOption.IGNORE_CASE,
        ).replace(text, " ")
        return when {
            Regex("""pro\+""", RegexOption.IGNORE_CASE).containsMatchIn(stripped) -> "Pro+"
            Regex("""\bultra\b""", RegexOption.IGNORE_CASE).containsMatchIn(stripped) -> "Ultra"
            Regex("""\bpro\b""", RegexOption.IGNORE_CASE).containsMatchIn(stripped) -> "Pro"
            Regex("""\bbusiness\b""", RegexOption.IGNORE_CASE).containsMatchIn(stripped) -> "Business"
            else -> ""
        }
    }

    internal fun displayWindow(raw: String): String {
        val n = raw.trim().lowercase().replace('_', ' ')
        if (n.contains("cursor") || n.contains("included") || n.contains("first party") ||
            n.contains("first-party")
        ) {
            return CURSOR_MODELS
        }
        if (n.contains("other") || n.contains("api") || n.contains("third")) {
            return OTHER_MODELS
        }
        if (n.isEmpty()) return "Included usage"
        return raw.trim().replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }

    internal fun parseReset(raw: String?, zone: ZoneId, now: Instant = Instant.now()): Instant? {
        if (raw.isNullOrBlank()) return null
        runCatching { return Instant.parse(raw) }
        val cleaned = raw.trim().replace(Regex("^Resets\\s+", RegexOption.IGNORE_CASE), "")
        val noParen = cleaned.replace(Regex("""\s*\([^)]*\)"""), "").trim()
        runCatching {
            return LocalDateTime.parse(noParen, resetDateTime).atZone(zone).toInstant()
        }
        runCatching {
            return LocalDate.parse(noParen, resetDate).atStartOfDay(zone).toInstant()
        }
        val monthDaySource = Regex(
            """(?:usage limits reset on\s+)?([A-Za-z]{3,9}\s+\d{1,2}(?:,\s+\d{4})?)""",
            RegexOption.IGNORE_CASE,
        ).find(noParen)?.groupValues?.get(1) ?: noParen
        runCatching {
            return LocalDate.parse(monthDaySource, resetDate).atStartOfDay(zone).toInstant()
        }
        return runCatching {
            val md = MonthDay.parse(monthDaySource, resetMonthDay)
            val today = now.atZone(zone).toLocalDate()
            var date = md.atYear(today.year)
            if (date.isBefore(today)) date = md.atYear(today.year + 1)
            date.atStartOfDay(zone).toInstant()
        }.getOrNull()
    }

    private fun windows(element: JsonElement?, zone: ZoneId, now: Instant): List<UsageWindow> {
        if (element !is JsonArray) return emptyList()
        val merged = linkedMapOf<String, Pair<Float, Instant?>>()
        element.jsonArray.forEach { item ->
            val obj = item as? JsonObject ?: return@forEach
            val name = obj.string("name") ?: obj.string("product") ?: obj.string("label") ?: return@forEach
            if (isGrokBotName(name)) return@forEach
            val percent = percentOf(obj, "percent")
                ?: percentOf(obj, "utilization")
                ?: return@forEach
            val label = displayWindow(name)
            val resets = parseReset(obj.string("resets_at") ?: obj.string("resetsAt"), zone, now)
            val previous = merged[label]
            merged[label] = (previous?.first ?: 0f) + percent to (resets ?: previous?.second)
        }
        if (merged.isEmpty()) return emptyList()
        return merged.entries
            .sortedWith(
                compareBy(
                    { preferredOrder.indexOf(it.key).let { index -> if (index < 0) preferredOrder.size else index } },
                    { it.key },
                ),
            )
            .map { (label, value) ->
                usageWindow(label, value.first.coerceIn(0f, 100f), value.second, now, zone)
            }
    }

    fun idleGrokBotWindows(): List<UsageWindow> = listOf(
        UsageWindow(
            name = GROK_BOT_WINDOW,
            usagePercent = 0f,
            elapsedPercent = 0f,
            resetLabel = "",
            elapsedLabel = ResetFormatter.elapsedLabel(0f),
        ),
    )

    private fun grokBotUsage(
        root: JsonObject,
        now: Instant,
        zone: ZoneId,
    ): UsageReadResult.GrokBotUsage? {
        val obj = root["grok_bot"] as? JsonObject ?: return null
        val percent = percentOf(obj, "percent") ?: percentOf(obj, "utilization") ?: return null
        val resets = parseReset(obj.string("resets_at") ?: obj.string("resetsAt"), zone, now)
        return UsageReadResult.GrokBotUsage(
            planLine = GROK_BOT_PLAN_VIA_CURSOR,
            windows = listOf(
                usageWindow(
                    GROK_BOT_WINDOW,
                    percent,
                    resets,
                    now,
                    zone,
                    ResetFormatter.SEVEN_DAYS,
                ),
            ),
        )
    }

    private fun isGrokBotName(raw: String): Boolean {
        val n = raw.trim().lowercase().replace('_', ' ')
        return n.contains("grok bot") || n.contains("grokbot")
    }

    private fun usageWindow(
        name: String,
        percent: Float,
        resetsAt: Instant?,
        now: Instant,
        zone: ZoneId,
        window: Duration = ResetFormatter.THIRTY_DAYS,
    ): UsageWindow {
        val elapsed = if (resetsAt != null) {
            ResetFormatter.elapsedPercent(resetsAt, now, window)
        } else {
            0f
        }
        return UsageWindow(
            name = name,
            usagePercent = percent.coerceIn(0f, 100f),
            elapsedPercent = elapsed,
            resetLabel = if (resetsAt != null) {
                ResetFormatter.resetLabel(resetsAt, now, zone)
            } else {
                ""
            },
            elapsedLabel = ResetFormatter.elapsedLabel(elapsed),
            resetsAtEpochMs = resetsAt?.toEpochMilli(),
        )
    }

    private fun percentOf(obj: JsonObject, key: String): Float? {
        val n = obj[key]?.jsonPrimitive?.floatOrNull ?: return null
        val scaled = if (key == "utilization" && n <= 1f) n * 100f else n
        if (scaled < 0f || scaled > 100f) return null
        return scaled
    }

    private fun planName(root: JsonObject): String {
        val direct = root.string("plan") ?: root.string("plan_name") ?: root.string("tier")
        if (!direct.isNullOrBlank()) return direct
        return planName(root["subscription"])
    }

    private fun planName(element: JsonElement?): String {
        if (element == null) return ""
        if (element is JsonObject) {
            val fromFields = element.string("plan")
                ?: element.string("plan_name")
                ?: element.string("tier")
                ?: element.string("name")
            if (!fromFields.isNullOrBlank()) return fromFields
            return ""
        }
        return runCatching { element.jsonPrimitive.contentOrNull }.getOrNull().orEmpty()
    }

    private fun JsonObject.string(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()?.takeIf { it.isNotBlank() }
}

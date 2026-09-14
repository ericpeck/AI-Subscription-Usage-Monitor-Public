package com.ericmbpeck.ai_sum.usage

import com.ericmbpeck.ai_sum.model.UsageWindow
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ClaudeUsageMapper {
    const val SESSION_NAME = "Current session"
    const val WEEKLY_NAME = "Weekly limit"
    const val FOOTNOTE = "Session windows are five hours long and start with your first message."

    private val sessionKeys = listOf("five_hour", "fiveHour")
    private val weeklyKeys = listOf("seven_day", "sevenDay")

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
        val usage = usageObject(root) ?: JsonObject(emptyMap())
        val planRaw = planFromPayload(root)
        if (isFreePlan(planRaw, usage)) {
            return UsageReadResult.FreePlan
        }
        val sessionObj = firstObject(usage, sessionKeys)
        val weeklyObj = firstObject(usage, weeklyKeys)
        if (sessionObj == null && weeklyObj == null) {
            return UsageReadResult.FreePlan
        }
        val session = sessionObj?.let {
            windowFrom(it, SESSION_NAME, ResetFormatter.FIVE_HOURS, now, zone)
        } ?: idleWindow(SESSION_NAME, ResetFormatter.FIVE_HOURS, now, zone)
        val weekly = weeklyObj?.let {
            windowFrom(it, WEEKLY_NAME, ResetFormatter.SEVEN_DAYS, now, zone)
        }
        val planLine = if (planRaw.isBlank()) {
            "Signed in · rolling session and weekly caps"
        } else {
            "${displayPlan(planRaw)} · rolling session and weekly caps"
        }
        return UsageReadResult.Success(
            planLine = planLine,
            windows = listOfNotNull(session, weekly),
        )
    }

    internal fun isFreePlan(planRaw: String, usage: JsonObject): Boolean {
        val n = planRaw.lowercase()
        if (n.isNotEmpty() &&
            Regex("(^|[^a-z])free([^a-z]|$)").containsMatchIn(n) &&
            !Regex("pro|max|team|enterprise").containsMatchIn(n)
        ) {
            return true
        }
        val five = firstObject(usage, sessionKeys)
        val seven = firstObject(usage, weeklyKeys)
        return five == null && seven == null && usage.isNotEmpty()
    }

    /**
     * Board 4a uses Max 5× as sample copy. Live 4a must show the account's
     * actual tier (Pro, Max 5×, Max 20×). subscription_details often has no
     * plan label — fall back to the org's rate_limit_tier.
     */
    internal fun displayPlan(raw: String): String {
        val compact = raw.trim().replace('_', ' ')
        if (compact.isEmpty()) return compact
        val n = compact.lowercase()
        return when {
            Regex("max\\s*20|20\\s*x").containsMatchIn(n) -> "Max 20×"
            Regex("max\\s*5|5\\s*x").containsMatchIn(n) -> "Max 5×"
            Regex("\\bmax\\b").containsMatchIn(n) && !n.contains("maximum") -> "Max 5×"
            n.contains("team") -> "Team"
            n.contains("enterprise") -> "Enterprise"
            Regex("\\bpro\\b").containsMatchIn(n) -> "Pro"
            else -> compact.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
    }

    internal fun looksLikePlan(raw: String): Boolean {
        val n = raw.trim().lowercase()
        if (n.isEmpty()) return false
        return Regex("pro|max|team|enterprise").containsMatchIn(n)
    }

    private fun windowFrom(
        obj: JsonObject,
        name: String,
        length: java.time.Duration,
        now: Instant,
        zone: ZoneId,
    ): UsageWindow {
        val utilization = percentOf(obj) ?: 0f
        val resetsAt = parseInstant(
            obj.string("resets_at")
                ?: obj.string("resetsAt")
                ?: obj.string("reset_at")
                ?: obj.string("resetAt"),
        )
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
            usagePercent = utilization.coerceIn(0f, 100f),
            elapsedPercent = elapsed,
            resetLabel = resetLabel,
            elapsedLabel = ResetFormatter.elapsedLabel(elapsed),
            resetsAtEpochMs = (resetsAt ?: now.plus(length)).toEpochMilli(),
        )
    }

    private fun idleWindow(
        name: String,
        length: java.time.Duration,
        now: Instant,
        zone: ZoneId,
    ): UsageWindow = windowFrom(JsonObject(emptyMap()), name, length, now, zone)

    private fun percentOf(obj: JsonObject): Float? {
        val keys = listOf(
            "utilization",
            "percent",
            "used_percent",
            "usage_percent",
            "percent_used",
        )
        for (key in keys) {
            val n = runCatching { obj[key]?.jsonPrimitive?.floatOrNull }.getOrNull() ?: continue
            val scaled = if (key == "utilization" && n <= 1f) n * 100f else n
            if (scaled in 0f..100f) return scaled
        }
        val used = runCatching { obj["used"]?.jsonPrimitive?.floatOrNull }.getOrNull()
        val limit = runCatching { obj["limit"]?.jsonPrimitive?.floatOrNull }.getOrNull()
            ?: runCatching { obj["allotted"]?.jsonPrimitive?.floatOrNull }.getOrNull()
        if (used != null && limit != null && limit > 0f) {
            return ((used / limit) * 100f).coerceIn(0f, 100f)
        }
        return null
    }

    private fun parseInstant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return runCatching { Instant.parse(raw) }.getOrNull()
    }

    private fun firstObject(usage: JsonObject, keys: List<String>): JsonObject? =
        keys.firstNotNullOfOrNull { usage.nested(it) }

    private fun usageObject(root: JsonObject): JsonObject? {
        val direct = root["usage"]
        if (direct is JsonObject) {
            if (firstObject(direct, sessionKeys) != null || firstObject(direct, weeklyKeys) != null) {
                return direct
            }
            val nested = direct["usage"]
            if (nested is JsonObject) return nested
            return direct
        }
        if (firstObject(root, sessionKeys) != null || firstObject(root, weeklyKeys) != null) {
            return root
        }
        return null
    }

    private fun planFromPayload(root: JsonObject): String {
        val candidates = listOf(
            planName(root["subscription"]),
            planName(root["organization"]),
            root.string("plan").orEmpty(),
        )
        return candidates.firstOrNull { looksLikePlan(it) }.orEmpty()
    }

    private fun planName(element: JsonElement?): String {
        if (element == null) return ""
        if (element is JsonObject) {
            val fromFields = element.string("plan")
                ?: element.string("plan_name")
                ?: element.string("subscription_plan")
                ?: element.string("tier")
                ?: element.string("rate_limit_tier")
                ?: element.string("rateLimitTier")
                ?: element.string("billing_plan")
                ?: element.string("billingPlan")
            if (!fromFields.isNullOrBlank() && looksLikePlan(fromFields)) return fromFields
            val nested = element["subscription"] ?: element["plan"] ?: element["settings"]
            if (nested is JsonObject) return planName(nested)
            return ""
        }
        return runCatching { element.jsonPrimitive.contentOrNull }.getOrNull().orEmpty()
    }

    private fun JsonObject.nested(key: String): JsonObject? =
        this[key] as? JsonObject

    private fun JsonObject.string(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()?.takeIf { it.isNotBlank() }
}

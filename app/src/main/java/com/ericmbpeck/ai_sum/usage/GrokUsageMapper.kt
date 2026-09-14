package com.ericmbpeck.ai_sum.usage

import com.ericmbpeck.ai_sum.model.UsageSegment
import com.ericmbpeck.ai_sum.model.UsageWindow
import java.time.Instant
import java.time.LocalDateTime
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

object GrokUsageMapper {
    const val WINDOW_NAME: String = "Weekly SuperGrok Limit"
    const val FOOTNOTE: String =
        "Voice, chat and coding all draw on the same weekly limit."

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val grokResetText: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.US)
    private val preferredOrder = listOf("Voice", "Chat", "Coding")
    private val segmentColors = longArrayOf(0xFF201E1D, 0xFF56524F, 0xFF8C8783)

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
        val products = products(root["products"])
        val productSum = products.sumOf { it.percent.toDouble() }.toFloat()
        val reported = percentOf(root, "percent")
            ?: percentOf(root, "utilization")
            ?: productSum.takeIf { products.isNotEmpty() }
            ?: return UsageReadResult.Failed("Could not read usage")
        val percent = if (products.isNotEmpty()) {
            maxOf(reported, productSum)
        } else {
            reported
        }
        val resetsAt = parseReset(root.string("resets_at") ?: root.string("resetsAt"), zone)
        val elapsed = if (resetsAt != null) {
            ResetFormatter.elapsedPercent(resetsAt, now, ResetFormatter.SEVEN_DAYS)
        } else {
            0f
        }
        val window = UsageWindow(
            name = WINDOW_NAME,
            usagePercent = percent.coerceIn(0f, 100f),
            elapsedPercent = elapsed,
            resetLabel = if (resetsAt != null) {
                ResetFormatter.resetLabel(resetsAt, now, zone)
            } else {
                ""
            },
            elapsedLabel = ResetFormatter.elapsedLabel(elapsed),
            segments = products,
            resetsAtEpochMs = resetsAt?.toEpochMilli(),
        )
        val planLine = if (planRaw.isBlank()) {
            "Signed in · one weekly limit, split by use"
        } else {
            "${displayPlan(planRaw)} · one weekly limit, split by use"
        }
        return UsageReadResult.Success(planLine = planLine, windows = listOf(window))
    }

    internal fun isFreePlan(planRaw: String): Boolean {
        val n = planRaw.lowercase()
        if (n.isEmpty()) return false
        if (Regex("super\\s*grok|heavy").containsMatchIn(n)) return false
        return Regex("(^|[^a-z])free([^a-z]|$)").containsMatchIn(n)
    }

    internal fun displayPlan(raw: String): String {
        val compact = raw.trim().replace('_', ' ')
        if (compact.isEmpty()) return compact
        val lower = compact.lowercase()
        if (lower.contains("super") && lower.contains("grok")) return "SuperGrok"
        if (lower == "heavy") return "Heavy"
        return compact.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }

    internal fun displayProduct(raw: String): String {
        val compact = raw.trim().replace('_', ' ')
        return when (compact.lowercase()) {
            "build", "coding", "code", "grok build" -> "Coding"
            "voice" -> "Voice"
            "chat", "grok" -> "Chat"
            "imagine" -> "Imagine"
            "api" -> "API"
            else -> compact.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        }
    }

    private fun products(element: JsonElement?): List<UsageSegment> {
        if (element !is JsonArray) return emptyList()
        val merged = linkedMapOf<String, Float>()
        element.jsonArray.forEach { item ->
            val obj = item as? JsonObject ?: return@forEach
            val name = obj.string("name") ?: obj.string("product") ?: obj.string("label") ?: return@forEach
            val percent = percentOf(obj, "percent")
                ?: percentOf(obj, "utilization")
                ?: return@forEach
            val label = displayProduct(name)
            merged[label] = (merged[label] ?: 0f) + percent
        }
        if (merged.isEmpty()) return emptyList()
        return merged.entries
            .map { (label, percent) -> label to percent.coerceIn(0f, 100f) }
            .sortedWith(
                compareBy(
                    { preferredOrder.indexOf(it.first).let { index -> if (index < 0) preferredOrder.size else index } },
                    { it.first },
                ),
            )
            .mapIndexed { index, (label, percent) ->
                UsageSegment(
                    label = label,
                    percent = percent,
                    color = colorFor(label, index),
                )
            }
    }

    private fun colorFor(label: String, index: Int): Long = when (label) {
        "Voice" -> segmentColors[0]
        "Chat" -> segmentColors[1]
        "Coding" -> segmentColors[2]
        else -> segmentColors[index % segmentColors.size]
    }

    private fun percentOf(obj: JsonObject, key: String): Float? {
        val n = obj[key]?.jsonPrimitive?.floatOrNull ?: return null
        val scaled = if (key == "utilization" && n <= 1f) n * 100f else n
        if (scaled < 0f || scaled > 100f) return null
        return scaled
    }

    internal fun parseReset(raw: String?, zone: ZoneId): Instant? {
        if (raw.isNullOrBlank()) return null
        runCatching { return Instant.parse(raw) }
        val cleaned = raw.trim().replace(Regex("^Resets\\s+", RegexOption.IGNORE_CASE), "")
        return runCatching {
            LocalDateTime.parse(cleaned, grokResetText).atZone(zone).toInstant()
        }.getOrNull()
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

package com.themalogroup.offlinevoiceagent

import org.json.JSONArray
import java.util.Locale

/**
 * Guardrails for short, explicit screen tasks planned by the on-device model.
 * It never accepts arbitrary code, URLs, coordinates, or unknown action names.
 */
object ScreenTaskPlanner {
    data class Step(val action: String, val value: String = "")

    private const val MAX_STEPS = 4
    private const val MAX_VALUE_CHARS = 180

    private val sensitiveTerms = listOf(
        "payment", "pay ", "pay now", "purchase", "buy ", "checkout", "bank", "upi",
        "transfer money", "send money", "password", "passcode", "pin code", "otp",
        "credit card", "debit card", "delete account", "factory reset", "uninstall",
        "security settings", "change password", "submit", "send ", "send message", "confirm",
        "delete", "place order", "make call", "call now", "publish", "post ", "share ",
        "transfer", "approve", "পেমেন্ট", "টাকা পাঠাও", "টাকা দাও", "বার্তা পাঠাও",
        "কল করো", "নিশ্চিত করো", "মুছে ফেল", "পোস্ট করো", "শেয়ার করো",
        "কেনাকাটা", "পাসওয়ার্ড", "ওটিপি", "অ্যাকাউন্ট মুছে"
    )

    fun isSensitiveGoal(goal: String): Boolean {
        val value = goal.lowercase(Locale.ROOT)
        return sensitiveTerms.any { value.contains(it) }
    }

    fun buildPrompt(goal: String, screen: String): String {
        val schema = """[{"action":"tap","value":"exact visible label"}]"""
        return "You are a cautious Android UI assistant. Plan a small task using only the visible screen tree.\n" +
            "User goal: " + goal.take(300) + "\nCurrent screen tree:\n" + screen.take(7000) + "\n\n" +
            "Return ONLY a JSON array with at most " + MAX_STEPS + " objects. Example: " + schema + "\n" +
            "Allowed actions are tap, type, scroll_down, scroll_up, back, read. Use empty value for actions other than tap/type.\n" +
            "Use only labels present in the screen tree. Do not invent controls. Never automate payment, purchase, banking, credentials, account/security, destructive or privacy-sensitive flows. " +
            "Never type passwords, PINs, OTPs, personal identifiers, payment data, or secrets. Never submit a form, send a message, make a call, purchase, delete, or confirm an irreversible action. " +
            "If the screen does not support the goal, return []. No markdown, commentary, URLs, coordinates, or extra keys."
    }

    fun parse(modelOutput: String): List<Step> {
        val start = modelOutput.indexOf('[')
        val end = modelOutput.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        val array = try { JSONArray(modelOutput.substring(start, end + 1)) }
        catch (_: Exception) { return emptyList() }
        if (array.length() !in 1..MAX_STEPS) return emptyList()
        val steps = ArrayList<Step>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: return emptyList()
            val action = item.optString("action").lowercase(Locale.ROOT)
            val value = item.optString("value").trim()
            if (value.length > MAX_VALUE_CHARS || value.contains("://") || value.contains('\n')) return emptyList()
            if (action !in setOf("tap", "type", "scroll_down", "scroll_up", "back", "read")) return emptyList()
            if (action in setOf("tap", "type") && value.isBlank()) return emptyList()
            if (action !in setOf("tap", "type") && value.isNotEmpty()) return emptyList()
            if ((action == "tap" || action == "type") && sensitiveTerms.any { value.lowercase(Locale.ROOT).contains(it) }) return emptyList()
            steps.add(Step(action, value))
        }
        return steps
    }
}

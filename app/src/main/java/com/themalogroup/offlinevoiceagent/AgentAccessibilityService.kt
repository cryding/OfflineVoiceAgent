package com.themalogroup.offlinevoiceagent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/** User-enabled local accessibility bridge for explicit screen-read and interaction requests. */
class AgentAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 150
        }
        instance = this
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { instance = this }
    override fun onInterrupt() {}
    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }
    private fun snapshot(): String {
        val root = rootInActiveWindow ?: return "No active screen is available."
        val lines = ArrayList<String>()
        fun walk(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || lines.size >= MAX_NODES || depth > 12) return
            val text = node.text?.toString()?.trim().orEmpty()
            val desc = node.contentDescription?.toString()?.trim().orEmpty()
            val id = node.viewIdResourceName.orEmpty()
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val labels = listOf(text, desc.takeIf { it != text }, id.takeIf { it.isNotBlank() })
                .filterNotNull().filter { it.isNotBlank() }.distinct()
            if (labels.isNotEmpty() || node.isClickable || node.isEditable || node.isScrollable) {
                val kind = listOf(if (node.isClickable) "clickable" else "", if (node.isEditable) "editable" else "", if (node.isScrollable) "scrollable" else "")
                    .filter { it.isNotBlank() }.joinToString(",")
                lines.add("${lines.size + 1}. ${labels.joinToString(" | ").ifBlank { "(unlabelled control)" }} [$kind] bounds=${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}")
            }
            for (i in 0 until node.childCount) walk(node.getChild(i), depth + 1)
        }
        walk(root, 0)
        return if (lines.isEmpty()) "The active screen exposes no readable text or controls."
        else "Visible screen elements (processed locally):\n" + lines.joinToString("\n")
    }
    companion object {
        private const val MAX_NODES = 80
        @Volatile private var instance: AgentAccessibilityService? = null
        fun readActiveScreen(): String? {
            val service = instance ?: return null
            return try { service.snapshot() } catch (_: Exception) { "Could not read the active screen." }
        }
        fun performOnActiveRoot(action: (AccessibilityNodeInfo) -> Boolean): Boolean? {
            val service = instance ?: return null
            val root = service.rootInActiveWindow ?: return false
            return try { action(root) } catch (_: Exception) { false }
        }
        fun tapText(query: String): String? {
            val service = instance ?: return null
            val root = service.rootInActiveWindow ?: return "No active screen is available."
            val target = query.trim().lowercase(Locale.ROOT)
            if (target.isBlank()) return "Provide a visible label."
            fun find(node: AccessibilityNodeInfo?, depth: Int): AccessibilityNodeInfo? {
                if (node == null || depth > 12) return null
                val text = node.text?.toString().orEmpty().lowercase(Locale.ROOT)
                val desc = node.contentDescription?.toString().orEmpty().lowercase(Locale.ROOT)
                if ((text.contains(target) || desc.contains(target)) && (node.isClickable || node.isFocusable)) return node
                for (i in 0 until node.childCount) {
                    val found = find(node.getChild(i), depth + 1)
                    if (found != null) return found
                }
                return null
            }
            var node = find(root, 0)
            while (node != null) {
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return "Tapped visible control: $query"
                node = node.parent
            }
            return "No clickable control matched: $query"
        }
        fun typeText(value: String): String? {
            val service = instance ?: return null
            val root = service.rootInActiveWindow ?: return "No active screen is available."
            fun findEditable(node: AccessibilityNodeInfo?, depth: Int): AccessibilityNodeInfo? {
                if (node == null || depth > 12) return null
                if (node.isEditable && node.isEnabled) return node
                for (i in 0 until node.childCount) {
                    val found = findEditable(node.getChild(i), depth + 1)
                    if (found != null) return found
                }
                return null
            }
            val field = findEditable(root, 0) ?: return "No editable text field is visible."
            field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value.take(2000)) }
            return if (field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) "Entered text in the visible field." else "Android rejected text entry for that field."
        }
    }
}

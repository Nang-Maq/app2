package com.example.desktopswitch

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class DesktopSiteService : AccessibilityService() {

  private var lastToggleMs = 0L
  private val cooldownMs = 4000L

  override fun onServiceConnected() {
    super.onServiceConnected()
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    val now = SystemClock.elapsedRealtime()
    if (now - lastToggleMs < cooldownMs) return
    val pkg = event?.packageName?.toString() ?: return

    // Jalankan hanya ketika Chrome aktif
    if (!pkg.startsWith("com.android.chrome")
        && !pkg.startsWith("org.chromium.chrome")
        && !pkg.startsWith("com.chrome.dev")) return

    // Coba toggle Desktop Site
    if (tryToggleDesktopSite()) {
      lastToggleMs = now
    }
  }

  override fun onInterrupt() {}

  private fun tryToggleDesktopSite(): Boolean {
    val root = rootInActiveWindow ?: return false

    // 1) buka overflow menu (3 titik)
    // Cari by viewId (stabil di beberapa build) atau contentDescription
    val menuCandidates = mutableListOf<AccessibilityNodeInfo>()
    menuCandidates += root.findNodesByViewIdSafe("com.android.chrome:id/menu_button")
    menuCandidates += root.findNodesByViewIdSafe("org.chromium.chrome:id/menu_button")
    menuCandidates += root.findNodesByTextOrDesc(listOf("More options", "Lainnya", "Opsi lainnya"))

    val opened = menuCandidates.any { it.clickSafe() }
    if (!opened) return false

    // beri jeda kecil agar menu muncul
    SystemClock.sleep(350)

    val root2 = rootInActiveWindow ?: return false

    // 2) klik item menu "Desktop site" / "Situs desktop"
    val item = root2.findNodesByTextOrDesc(
      listOf("Desktop site", "Situs desktop")
    ).firstOrNull()

    return item?.clickSafe() ?: false
  }

  // Helpers
  private fun AccessibilityNodeInfo.clickSafe(): Boolean {
    var node: AccessibilityNodeInfo? = this
    // bubble up cari clickable parent
    var hops = 0
    while (node != null && !node.isClickable && hops < 5) {
      node = node.parent
      hops++
    }
    return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
  }

  private fun AccessibilityNodeInfo.findNodesByViewIdSafe(id: String): List<AccessibilityNodeInfo> {
    return try { findAccessibilityNodeInfosByViewId(id) ?: emptyList() } catch (_: Throwable) { emptyList() }
  }

  private fun AccessibilityNodeInfo.findNodesByTextOrDesc(strings: List<String>): List<AccessibilityNodeInfo> {
    val out = mutableListOf<AccessibilityNodeInfo>()
    for (s in strings) {
      try {
        out += (findAccessibilityNodeInfosByText(s) ?: emptyList())
      } catch (_: Throwable) {}
    }
    // brute: DFS cari contentDescription match
    fun dfs(n: AccessibilityNodeInfo?) {
      if (n == null) return
      val cd = n.contentDescription?.toString() ?: ""
      if (strings.any { cd.equals(it, ignoreCase = true) }) out += n
      for (i in 0 until n.childCount) dfs(n.getChild(i))
    }
    dfs(this)
    return out
  }
}

package top.niunaijun.blackboxa.skyshard

import android.content.Context
import android.content.SharedPreferences
import top.niunaijun.blackboxa.app.App

/**
 * SkyShard-side per-shard state. Keyed by (userId, packageName). All state
 * lives in a single SharedPreferences file so backup/restore is one read/write.
 *
 * The engine ([BlackBoxCore]) owns "what packages are in which user space."
 * SkyShard owns the cosmetic and behavioral overlays on top of that.
 */
object SkyShardPrefs {
    private const val PREFS_NAME = "SkyShardOverlay"

    private const val K_LABEL = "label"
    private const val K_COLOR = "color"
    private const val K_FROZEN = "frozen"

    private val prefs: SharedPreferences
        get() = App.getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(userId: Int, pkg: String, field: String) = "$userId/$pkg/$field"

    fun getLabel(userId: Int, pkg: String): String? =
        prefs.getString(key(userId, pkg, K_LABEL), null)?.takeIf { it.isNotBlank() }

    fun setLabel(userId: Int, pkg: String, label: String?) {
        prefs.edit().apply {
            if (label.isNullOrBlank()) remove(key(userId, pkg, K_LABEL))
            else putString(key(userId, pkg, K_LABEL), label.trim())
        }.apply()
    }

    /** Returns 0 (transparent / "no color") when no override is set. */
    fun getColor(userId: Int, pkg: String): Int =
        prefs.getInt(key(userId, pkg, K_COLOR), 0)

    fun setColor(userId: Int, pkg: String, color: Int) {
        prefs.edit().apply {
            if (color == 0) remove(key(userId, pkg, K_COLOR))
            else putInt(key(userId, pkg, K_COLOR), color)
        }.apply()
    }

    fun isFrozen(userId: Int, pkg: String): Boolean =
        prefs.getBoolean(key(userId, pkg, K_FROZEN), false)

    fun setFrozen(userId: Int, pkg: String, frozen: Boolean) {
        prefs.edit().apply {
            if (frozen) putBoolean(key(userId, pkg, K_FROZEN), true)
            else remove(key(userId, pkg, K_FROZEN))
        }.apply()
    }

    /**
     * Returns one record per (userId, packageName) seen across any tracked
     * field. Used for backup export.
     */
    fun allEntries(): List<Entry> {
        val byKey = mutableMapOf<Pair<Int, String>, Entry>()
        for ((k, v) in prefs.all) {
            val parts = k.split('/', limit = 3)
            if (parts.size != 3) continue
            val userId = parts[0].toIntOrNull() ?: continue
            val pkg = parts[1]
            val field = parts[2]
            val entry = byKey.getOrPut(userId to pkg) { Entry(userId, pkg) }
            when (field) {
                K_LABEL -> entry.label = v as? String
                K_COLOR -> entry.color = (v as? Int) ?: 0
                K_FROZEN -> entry.frozen = (v as? Boolean) ?: false
            }
        }
        return byKey.values.toList()
    }

    /** Replaces all stored entries with the given set. Used for restore. */
    fun replaceAll(entries: List<Entry>) {
        val edit = prefs.edit().clear()
        for (e in entries) {
            if (!e.label.isNullOrBlank()) edit.putString(key(e.userId, e.packageName, K_LABEL), e.label)
            if (e.color != 0) edit.putInt(key(e.userId, e.packageName, K_COLOR), e.color)
            if (e.frozen) putBoolean(edit, key(e.userId, e.packageName, K_FROZEN), true)
        }
        edit.apply()
    }

    private fun putBoolean(edit: SharedPreferences.Editor, k: String, v: Boolean) {
        edit.putBoolean(k, v)
    }

    data class Entry(
        val userId: Int,
        val packageName: String,
        var label: String? = null,
        var color: Int = 0,
        var frozen: Boolean = false,
    )
}

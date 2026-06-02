package top.niunaijun.blackboxa.skyshard

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON backup / restore for SkyShard overlay state (labels, colors, frozen
 * flags). The cloned APKs themselves are NOT in the file — restore on a fresh
 * device means: reinstall the same APKs into SkyShard first, then restore the
 * JSON to bring labels/colors/frozen back.
 *
 * File format:
 *   {
 *     "schema": "skyshard-overlay-v1",
 *     "exportedAt": "<ISO-ish timestamp string from system>",
 *     "entries": [
 *       { "userId": 0, "pkg": "com.whatsapp", "label": "Mom", "color": -65536, "frozen": false },
 *       ...
 *     ]
 *   }
 */
object BackupManager {

    const val SCHEMA = "skyshard-overlay-v1"

    fun export(ctx: Context, dest: Uri): Result<Int> = runCatching {
        val obj = JSONObject().apply {
            put("schema", SCHEMA)
            put("exportedAt", System.currentTimeMillis())
            val arr = JSONArray()
            SkyShardPrefs.allEntries().forEach { e ->
                arr.put(JSONObject().apply {
                    put("userId", e.userId)
                    put("pkg", e.packageName)
                    e.label?.let { put("label", it) }
                    if (e.color != 0) put("color", e.color)
                    if (e.frozen) put("frozen", true)
                })
            }
            put("entries", arr)
        }
        ctx.contentResolver.openOutputStream(dest, "wt").use { out ->
            requireNotNull(out) { "Could not open destination for writing" }
            out.write(obj.toString(2).toByteArray(Charsets.UTF_8))
        }
        obj.getJSONArray("entries").length()
    }

    fun import(ctx: Context, src: Uri): Result<Int> = runCatching {
        val text = ctx.contentResolver.openInputStream(src).use { input ->
            requireNotNull(input) { "Could not open source for reading" }
            input.readBytes().toString(Charsets.UTF_8)
        }
        val obj = JSONObject(text)
        val schema = obj.optString("schema")
        require(schema == SCHEMA) { "Unrecognized backup schema: $schema" }

        val arr = obj.getJSONArray("entries")
        val entries = mutableListOf<SkyShardPrefs.Entry>()
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            entries += SkyShardPrefs.Entry(
                userId = e.getInt("userId"),
                packageName = e.getString("pkg"),
                label = if (e.has("label")) e.getString("label") else null,
                color = e.optInt("color", 0),
                frozen = e.optBoolean("frozen", false),
            )
        }
        SkyShardPrefs.replaceAll(entries)
        entries.size
    }
}

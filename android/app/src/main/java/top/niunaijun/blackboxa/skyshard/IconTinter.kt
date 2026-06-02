package top.niunaijun.blackboxa.skyshard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap

/**
 * Overlays a small colored ring on the top-right corner of an app icon,
 * plus a faint frozen veil if frozen. Pure pixel pushing — no system APIs.
 *
 * Used in two places:
 * - The SkyShard app list (to mark which shards have a custom color/freeze).
 * - The home-screen shortcut bitmap (so the badge survives onto the launcher).
 */
object IconTinter {

    private const val DEFAULT_SIZE = 144

    fun tint(source: Drawable, color: Int, frozen: Boolean): Drawable {
        val size = maxOf(source.intrinsicWidth, source.intrinsicHeight, DEFAULT_SIZE)
        val bmp = source.toBitmap(size, size, Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bmp)

        if (frozen) {
            // 35% black veil over the whole icon
            val veil = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.argb(89, 0, 0, 0) }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), veil)
        }

        if (color != 0) {
            val radius = size * 0.18f
            val cx = size - radius - size * 0.06f
            val cy = radius + size * 0.06f
            // Outer black ring for contrast against any icon background
            val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.BLACK }
            canvas.drawCircle(cx, cy, radius + 4f, outer)
            // Filled colored dot
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            canvas.drawCircle(cx, cy, radius, dot)
        }

        return BitmapDrawable(null, bmp)
    }

    /** Convenience: read the prefs and tint. */
    fun tintForShard(source: Drawable, userId: Int, packageName: String): Drawable {
        val color = SkyShardPrefs.getColor(userId, packageName)
        val frozen = SkyShardPrefs.isFrozen(userId, packageName)
        return if (color == 0 && !frozen) source else tint(source, color, frozen)
    }
}

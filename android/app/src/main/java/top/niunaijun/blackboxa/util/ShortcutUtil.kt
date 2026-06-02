package top.niunaijun.blackboxa.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.App
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.util.ContextUtil.openAppSystemSettings
import top.niunaijun.blackboxa.view.main.ShortcutActivity
import top.niunaijun.blackboxa.skyshard.IconTinter
import top.niunaijun.blackboxa.skyshard.SkyShardPrefs


object ShortcutUtil {


    
    fun createShortcut(context: Context,userID: Int, info: AppInfo) {

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            val overlayLabel = SkyShardPrefs.getLabel(userID, info.packageName)
            val labelName = overlayLabel ?: (info.name + " " + (userID + 1))
            val intent = Intent(context, ShortcutActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .putExtra("pkg", info.packageName)
                .putExtra("userId", userID)
            MaterialDialog(context).show {
                title(res = R.string.app_shortcut)
                input(
                    hintRes = R.string.shortcut_name,
                    prefill = labelName
                ) { _, input ->

                    val baseIcon = info.icon
                    val tintedBitmap = if (baseIcon != null)
                        IconTinter.tintForShard(baseIcon, userID, info.packageName).toBitmap()
                    else baseIcon?.toBitmap()

                    val builder = ShortcutInfoCompat.Builder(context, info.packageName + userID)
                        .setIntent(intent)
                        .setShortLabel(input)
                        .setLongLabel(input)
                    if (tintedBitmap != null) builder.setIcon(IconCompat.createWithBitmap(tintedBitmap))

                    ShortcutManagerCompat.requestPinShortcut(context, builder.build(), null)
                    showAllowPermissionDialog(context)
                }
                positiveButton(R.string.done)
                negativeButton(R.string.cancel)
            }

        } else {
            toast(R.string.cannot_create_shortcut)
        }
    }

    private fun showAllowPermissionDialog(context: Context){
        if (!AppManager.mBlackBoxLoader.showShortcutPermissionDialog()){
            return
        }

        MaterialDialog(context).show {
            title(R.string.try_add_shortcut)
            message(R.string.add_shortcut_fail_msg)
            positiveButton(R.string.done)
            negativeButton(R.string.permission_setting){
                App.getContext().openAppSystemSettings()
            }

            neutralButton(R.string.no_reminders){
                AppManager.mBlackBoxLoader.invalidShortcutPermissionDialog(false)
            }
        }

    }
}
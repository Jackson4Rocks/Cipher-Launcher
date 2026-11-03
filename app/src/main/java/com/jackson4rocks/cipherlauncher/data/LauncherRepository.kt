package com.jackson4rocks.cipherlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: android.graphics.drawable.Drawable
)

class LauncherRepository(private val context: Context) {

    fun apps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return context.packageManager
            .queryIntentActivities(intent, 0)
            .mapNotNull(::toLaunchableApp)
            .sortedBy { it.label.lowercase() }
    }

    fun launch(app: LaunchableApp) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun toLaunchableApp(info: ResolveInfo): LaunchableApp? {
        val activity = info.activityInfo ?: return null
        if (activity.packageName == context.packageName) return null

        return LaunchableApp(
            label = info.loadLabel(context.packageManager).toString(),
            packageName = activity.packageName,
            activityName = activity.name,
            icon = info.loadIcon(context.packageManager)
        )
    }
}

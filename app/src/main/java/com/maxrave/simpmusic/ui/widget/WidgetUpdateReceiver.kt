package com.maxrave.simpmusic.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.ActivityManager
import android.appwidget.AppWidgetManager
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

class WidgetUpdateReceiver : BroadcastReceiver() {
    @OptIn(UnstableApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.appwidget.action.APPWIDGET_UPDATE") {
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: intArrayOf()

            if (!isAppRunning(context)) {
                BasicWidget.instance.defaultAppWidget(context, appWidgetIds)
            }
        }
    }

    private fun isAppRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(Int.MAX_VALUE)

        for (task in runningTasks) {
            if (task.baseActivity?.packageName == context.packageName) {
                return true
            }
        }
        return false
    }
}

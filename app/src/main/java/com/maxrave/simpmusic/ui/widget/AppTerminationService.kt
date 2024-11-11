package com.maxrave.simpmusic.ui.widget
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.ui.widget.BasicWidget

class AppTerminationService : Service() {

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, BasicWidget::class.java))
        BasicWidget.instance.defaultAppWidget(this, appWidgetIds)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
package com.maxrave.simpmusic.ui.widget

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@UnstableApi
class BasicWidget : BaseAppWidget() {


    public override fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
        val appWidgetView = RemoteViews(
            context.packageName, R.layout.app_widget_base
        )

        appWidgetView.setViewVisibility(
            R.id.media_titles,
            View.INVISIBLE
        )
        appWidgetView.setViewVisibility(R.id.button_toggle_play_pause, View.INVISIBLE)
        appWidgetView.setViewVisibility(R.id.button_next, View.INVISIBLE)
        appWidgetView.setViewVisibility(R.id.button_prev, View.INVISIBLE)
        appWidgetView.setImageViewResource(R.id.image, R.drawable.holder_video)
        appWidgetView.setImageViewResource(
            R.id.button_toggle_play_pause, R.drawable.play_widget
        )
        appWidgetView.setImageViewResource(
            R.id.button_next, R.drawable.next_widget
        )
        appWidgetView.setImageViewResource(
            R.id.button_prev, R.drawable.previous_widget
        )
        appWidgetView.setImageViewResource(
            R.id.logo, R.mipmap.ic_launcher_round
        )



        setOpenAppPendingIntent(context, appWidgetView)

        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    override fun performUpdate(
        context: Context,
        handler: SimpleMediaServiceHandler,
        appWidgetIds: IntArray?
    ) {

        val appWidgetView = RemoteViews(
            context.packageName, R.layout.app_widget_base
        )
        val isPlaying = handler.player.isPlaying
        val song = runBlocking { handler.nowPlaying.first() }

        if (song?.mediaMetadata?.title.isNullOrEmpty() && song?.mediaMetadata?.artist.isNullOrEmpty()) {
            appWidgetView.setViewVisibility(
                R.id.media_titles,
                View.INVISIBLE
            )
        } else {
            appWidgetView.setViewVisibility(
                R.id.media_titles,
                View.VISIBLE
            )
            appWidgetView.setTextViewText(R.id.title, song?.mediaMetadata?.title)
            appWidgetView.setTextViewText(
                R.id.text,
                song?.mediaMetadata?.artist
            )
        }

        appWidgetView.setImageViewResource(
            R.id.button_next, R.drawable.next_widget
        )
        appWidgetView.setImageViewResource(
            R.id.button_prev, R.drawable.previous_widget
        )
        appWidgetView.setImageViewResource(
            R.id.button_toggle_play_pause,
            if (!isPlaying) R.drawable.play_widget else R.drawable.pause_widget
        )
        appWidgetView.setImageViewResource(
            R.id.logo, R.mipmap.ic_launcher_round
        )

        setOpenAppPendingIntent(context, appWidgetView)
        linkButtons(context, appWidgetView)

        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    fun updateImage(context: Context, bitmap: Bitmap) {
        Log.w("BasicWidget", "updateImage")
        val appWidgetView = RemoteViews(
            context.packageName, R.layout.app_widget_base
        )
        appWidgetView.setImageViewBitmap(R.id.image, bitmap)
        pushUpdatePartially(context, appWidgetView)
    }

    fun updatePlayingState(context: Context, isPlaying: Boolean) {
        val appWidgetView = RemoteViews(
            context.packageName, R.layout.app_widget_base
        )
        appWidgetView.setImageViewResource(
            R.id.button_toggle_play_pause,
            if (!isPlaying) R.drawable.play_widget else R.drawable.pause_widget
        )
        pushUpdatePartially(context, appWidgetView)
    }

    @UnstableApi
    private fun linkButtons(context: Context, views: RemoteViews) {
        val action = Intent(context, MainActivity::class.java)
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingIntent = PendingIntent.getActivity(
            context, 0, action,
            PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.clickable_area, pendingIntent)
    }

    private fun setOpenAppPendingIntent(context: Context, views: RemoteViews) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)
    }

    companion object {
        const val NAME: String = "basic_widget"
        const val ACTION_TOGGLE_PAUSE = "com.maxrave.simpmusic.action.TOGGLE_PAUSE"
        const val ACTION_REWIND = "com.maxrave.simpmusic.action.REWIND"
        const val ACTION_SKIP = "com.maxrave.simpmusic.action.SKIP"
        private var mInstance: BasicWidget? = null

        val instance: BasicWidget
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance = BasicWidget()
                }
                return mInstance!!
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
package com.maxrave.simpmusic.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.maxrave.simpmusic.R;

public class NotificationHelper extends ContextWrapper {

    public static final String CHANNEL_ID = "test_notification_channel";
    public static final String CHANNEL_NAME = "PureMusic";

    public NotificationHelper(Context base) {
        super(base);
        createChannels();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for test notifications");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public NotificationCompat.Builder getNotification(String name, String text, PendingIntent pendingIntent) {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(name)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Ensures the notification is dismissed when clicked
    }
}
package com.maxrave.simpmusic.utils;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media3.common.util.UnstableApi;

import com.maxrave.simpmusic.ui.MainActivity;

import java.util.ArrayList;
import java.util.Random;

public class NotificationReceiver extends BroadcastReceiver {

    @OptIn(markerClass = UnstableApi.class)
    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        // Define your ArrayLists (or arrays) for titles and descriptions
        ArrayList<String> titles = new ArrayList<>();
        titles.add("Explore New Music");
        titles.add("Your Favorite Artist's New Album");
        titles.add("Daily Download Special");
        titles.add("Weekly Top Hits");
        titles.add("Recommended Playlist");
        // Add more titles as needed

        ArrayList<String> descriptions = new ArrayList<>();
        descriptions.add("Discover fresh tracks just added to our collection. Open the app to listen!");
        descriptions.add("Get the latest album from your favorite artist. Open the app to start downloading!");
        descriptions.add("Today's special download is available. Tap to claim it!");
        descriptions.add("Check out this week's top hits. Open the app to start downloading!");
        descriptions.add("We've curated a playlist just for you. Open the app to listen now!");
        // Add more descriptions as needed

        // Randomly select an index for title and description
        Random random = new Random();
        int titleIndex = random.nextInt(titles.size());
        int descriptionIndex = random.nextInt(descriptions.size());

        // Get the selected title and description
        String selectedTitle = titles.get(titleIndex);
        String selectedDescription = descriptions.get(descriptionIndex);

        // Create notification helper and pending intent
        NotificationHelper notificationHelper = new NotificationHelper(context);
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build the notification using selected title and description
        NotificationCompat.Builder builder = notificationHelper.getNotification(selectedTitle, selectedDescription, pendingIntent);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(2004, builder.build());
    }
}
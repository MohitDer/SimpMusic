package com.maxrave.simpmusic.downloader.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.maxrave.simpmusic.R;

import java.io.File;

public class AppSettings {

    private AppSettings() {
    }

    public static void initSettings(Context context) {
        getVideoDownloadFolder(context);
        getAudioDownloadFolder(context);
    }

    private static void getVideoDownloadFolder(Context context) {
        getFolder(context, R.string.download_path_video_key, Environment.DIRECTORY_MOVIES);
    }

    private static void getAudioDownloadFolder(Context context) {
        getFolder(context, R.string.download_path_audio_key, Environment.DIRECTORY_MUSIC);
    }

    private static void getFolder(Context context, int keyID, String defaultDirectoryName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String key = context.getString(keyID);
        String downloadPath = prefs.getString(key, null);
        if (downloadPath != null && !downloadPath.isEmpty()) return;

        SharedPreferences.Editor spEditor = prefs.edit();
        spEditor.putString(key, getChildFolderPathForDir(getFolder(defaultDirectoryName)));
        spEditor.apply();
    }

    @NonNull
    public static File getFolder(String defaultDirectoryName) {
        return new File(Environment.getExternalStorageDirectory(), defaultDirectoryName);
    }

    public static String getChildFolderPathForDir(File dir) {
        return new File(dir, "PureMuzic").toURI().toString();
    }
}

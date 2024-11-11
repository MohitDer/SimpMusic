package com.maxrave.simpmusic.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import java.lang.reflect.Type;

import javax.annotation.Nullable;

public class SharedPrefsHelper {

    public enum Key {
        INTERSTITIAL_CAP_COUNTER_SEARCH,
        INTERSTITIAL_CAP_SEARCH,
        Native_Home,
        Native_Album,
        Native_Lang,
        Native_Guide,
        Native_Playlist,
        Native_Artists,
        Native_MostPlayed,
        Native_Favourite ,
        Native_Followed,
        Native_Library,
        Native_Search,
        Banner_PlayerView,
        Banner_Offline_Music,
        Banner_Offline_Video,
        Native_Offline_Music,
        Interstitial_Download_Click,
        Interstitial_Search,
        Interstitial_Artists,
        Interstitial_Songs,
        Interstitial_Albums,
        Interstitial_Guide,
        Open_App,
    }

    public static void setStringPrefs(Context context, String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getStringPrefs(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, "");
    }

    public static String getStringPrefs(Context context, String key, String defValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, defValue);
    }

    public static void setIntPrefs(Context context, String key, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static int getIntPrefs(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(key, 0);
    }

    public static void setLongPrefs(Context context, String key, long value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static long getLongPrefs(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(key, 0);
    }

    public static long getLongPrefs(Context context, String key, long defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(key, defaultValue);
    }

    public static void setBooleanPrefs(Context context, String key, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static Boolean getBooleanPrefs(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(key, false);
    }

    public static void saveObjectPrefs(Context mContext, String objectName, Object object) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(object);
        editor.putString(objectName, json);

        editor.apply();
    }

    public static Object loadObjectPrefs(Context mContext, String objectName, Type type) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Gson gson = new Gson();
        String objectString = prefs.getString(objectName, "");
        return gson.fromJson(objectString, type);
    }

    public static void clearPrefs(Context mContext) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear();
        editor.apply();
    }

    public static void removePrefs(Context mContext, String key) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.remove(key);
        editor.apply();
    }
}

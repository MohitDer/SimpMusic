package com.maxrave.simpmusic.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LanguagePreference {
    private static final String PREF_NAME = "LanguagePreference";
    private static final String KEY_SELECTED_LANGUAGE = "selected_language_code";

    public static String getSelectedLanguage(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_SELECTED_LANGUAGE, "");
    }

    public static void setSelectedLanguage(Context context, String languageCode) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_SELECTED_LANGUAGE, languageCode);
        editor.apply();
    }

    public static String getLanguageTag(Context context) {
        String languageCode = getSelectedLanguage(context);
        Locale locale = new Locale(languageCode);
        return locale.toLanguageTag();
    }

    public static String getCountry(Context context) {
        String languageCode = getSelectedLanguage(context);
        Locale locale = new Locale(languageCode);
        return locale.getCountry();
    }

    public static void setAppLanguage(Context context,String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.locale = locale;

        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
}

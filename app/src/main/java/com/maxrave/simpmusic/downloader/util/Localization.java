package com.maxrave.simpmusic.downloader.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.annimon.stream.Stream;

import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.Decade;
import org.schabi.newpipe.extractor.localization.ContentCountry;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Localization {

    public final static String DOT_SEPARATOR = "・";

    private Localization() {
    }

    public static void init() {
        initPrettyTime();
    }

    @NonNull
    public static String concatenateStrings(final String... strings) {
        return concatenateStrings(Stream.of(Arrays.asList(strings)).filter(value -> !TextUtils.isEmpty(value)).toList());
    }

    @NonNull
    public static String concatenateStrings(final List<String> strings) {
        if (strings.isEmpty()) return "";

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(strings.get(0));

        for (int i = 1; i < strings.size(); i++) {
            final String string = strings.get(i);
            if (!TextUtils.isEmpty(string)) {
                stringBuilder.append(DOT_SEPARATOR).append(strings.get(i));
            }
        }
        return stringBuilder.toString();
    }

    public static org.schabi.newpipe.extractor.localization.Localization getPreferredLocalization(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String languageCode = sharedPreferences.getString("en", Locale.getDefault().getLanguage());

        return org.schabi.newpipe.extractor.localization.Localization.fromLocalizationCode(languageCode).orElse(org.schabi.newpipe.extractor.localization.Localization.DEFAULT);
    }

    public static ContentCountry getPreferredContentCountry(final Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String countryCode = sharedPreferences.getString("US", Locale.getDefault().getCountry());
        return new ContentCountry(countryCode);
    }

    private static void initPrettyTime() {
        PrettyTime prettyTime = new PrettyTime(Locale.getDefault());
        // Do not use decades as YouTube doesn't either.
        prettyTime.removeUnit(Decade.class);
    }

    public static String getDurationString(long duration) {
        if (duration < 0) {
            duration = 0;
        }
        String output;
        long days = duration / (24 * 60 * 60L); /* greater than a day */
        duration %= (24 * 60 * 60L);
        long hours = duration / (60 * 60L); /* greater than an hour */
        duration %= (60 * 60L);
        long minutes = duration / 60L;
        long seconds = duration % 60L;

        //handle days
        if (days > 0) {
            output = String.format(Locale.US, "%d:%02d:%02d:%02d", days, hours, minutes, seconds);
        } else if (hours > 0) {
            output = String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            output = String.format(Locale.US, "%d:%02d", minutes, seconds);
        }
        return output;
    }
}



package com.maxrave.simpmusic.utils;

import android.content.Context;
import android.content.SharedPreferences;


public class Pref {

    public static void setPremium(Context context, int premiumValue) {
        SharedPreferences prefs = context.getSharedPreferences("PremiumPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("premium", premiumValue);
        editor.apply();
    }

    public static int getPremium(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("YourPrefsName", Context.MODE_PRIVATE);
        return prefs.getInt("premium", 0);
    }

}

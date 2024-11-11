package com.maxrave.simpmusic.ads.admob;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.maxrave.simpmusic.ads.config.AdConfig;
import com.maxrave.simpmusic.utils.SharedPrefsHelper;

import java.util.concurrent.TimeUnit;

public class AdMobInterstitialAdSong {
    private static AdMobInterstitialAdSong mInstance;
    private InterstitialAd mInterstitialAd;
    private AdClosedListener mAdClosedListener;
    private int retryAttempt;
    private int clickCount = 0;
    
    private String TAG = "TAG";

    private static final int INTERSTITIAL_CAP_DEFAULT = 2;

    public interface AdClosedListener {
        void onAdClosed();
    }

    public static AdMobInterstitialAdSong getInstance() {
        if (mInstance == null) {
            mInstance = new AdMobInterstitialAdSong();
        }
        return mInstance;
    }

    public void init(Activity activity) {
        Log.d(TAG, "init: ");
        loadInterstitialAd(activity);
    }

    private void loadInterstitialAd(Activity activity) {
        Log.d(TAG, "loadInterstitialAd: ");
        AdConfig adConfig = (AdConfig) SharedPrefsHelper.loadObjectPrefs(activity, SharedPrefsHelper.Key.Interstitial_Songs.name(), AdConfig.class);
        if (adConfig != null && adConfig.Show) {
            Log.d(TAG, "loadInterstitialAd: adConfig != null && adConfig.Show");
            String adId = adConfig.Ad_Id;
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(activity, adId, adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    // The mInterstitialAd reference will be null until an ad is loaded.
                    mInterstitialAd = interstitialAd;

                    Log.d(TAG, "onAdLoaded: ");

                    // Reset retry attempt
                    retryAttempt = 0;

                    interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Called when fullscreen content is dismissed.
                            // Make sure to set your reference to null so you don't show it a second time.
                            mInterstitialAd = null;
                            if (mAdClosedListener != null) {
                                mAdClosedListener.onAdClosed();
                            }
                            loadInterstitialAd(activity);
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            // Called when fullscreen content failed to show.
                            // Make sure to set your reference to null so you don't show it a second time.
                            mInterstitialAd = null;
                            loadInterstitialAd(activity);
                            Log.d(TAG, "onAdFailedToShowFullScreenContent: ");
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            // Called when fullscreen content is shown.
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    // Handle the error
                    mInterstitialAd = null;
                    retryAttempt++;
                    long delayMillis = TimeUnit.SECONDS.toMillis((long) Math.pow(2, Math.min(6, retryAttempt)));
                    new Handler(Looper.getMainLooper()).postDelayed(() -> loadInterstitialAd(activity), delayMillis);
                    Log.d(TAG, "onAdFailedToLoad: ");
                }
            });
        }
    }

    public void showInterstitialAd(Activity activity, AdClosedListener mAdClosedListener) {
        this.mAdClosedListener = mAdClosedListener;
        clickCount++;
        if (clickCount % 2 == 0) {  // Show ad every 2 clicks
            AdConfig adConfig = (AdConfig) SharedPrefsHelper.loadObjectPrefs(activity, SharedPrefsHelper.Key.Interstitial_Songs.name(), AdConfig.class);
            if (adConfig != null && adConfig.Show && mInterstitialAd != null) {
                mInterstitialAd.show(activity);
            } else {
                mAdClosedListener.onAdClosed();
            }
        } else {
            mAdClosedListener.onAdClosed();
        }
    }
}

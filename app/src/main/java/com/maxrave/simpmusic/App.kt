package com.maxrave.simpmusic

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.util.UnstableApi
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.maxrave.simpmusic.App.OnShowAdCompleteListener
import com.maxrave.simpmusic.ads.GoogleMobileAdsConsentManager
import com.maxrave.simpmusic.ads.config.AdConfig
import com.maxrave.simpmusic.downloader.DownloaderImpl
import com.maxrave.simpmusic.downloader.ExtractorHelper
import com.maxrave.simpmusic.downloader.util.AppSettings
import com.maxrave.simpmusic.downloader.util.Localization
import com.maxrave.simpmusic.ui.MainActivity
import com.maxrave.simpmusic.utils.LanguagePreference
import com.maxrave.simpmusic.utils.SharedPrefsHelper
import com.maxrave.simpmusic.utils.Utils
import com.maxrave.simpmusic.utils.Utils.showRateDialog
import dagger.hilt.android.HiltAndroidApp
import io.reactivex.exceptions.CompositeException
import io.reactivex.exceptions.MissingBackpressureException
import io.reactivex.exceptions.OnErrorNotImplementedException
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.functions.Consumer
import io.reactivex.plugins.RxJavaPlugins
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketException
import java.util.Date
import java.util.Objects

@HiltAndroidApp
open class App : Application(), LifecycleObserver, Application.ActivityLifecycleCallbacks {

    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null
    private var mDatabase: DatabaseReference? = null
    private var dataRef: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null

    private val downloadCompleteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("DownloadComplete", "downloadCompleteReceiver : DownloadComplete")
            if (!currentActivity!!.isDestroyed) {
                showRateDialog(currentActivity)
            }
        }
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)


        // Initialize firebase
        FirebaseApp.initializeApp(this)
        mDatabase = FirebaseDatabase.getInstance().reference

        registerActivityLifecycleCallbacks(this)

        queryAdConfigValueListener()

        // AdMob
        MobileAds.initialize(this) {
            // AdMob SDK is initialized, start loading ads
            val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(
                    listOf(
                        "85E5BA400C22977D0F4006C8A64A264A"
                    )
                )
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()

        val adConfig = SharedPrefsHelper.loadObjectPrefs(
            this@App, SharedPrefsHelper.Key.Open_App.name,
            AdConfig::class.java
        ) as? AdConfig
        if (adConfig != null && adConfig.Show) {
            appOpenAdManager.loadAd(this@App)
        }

        SharedPrefsHelper.removePrefs(
            this,
            SharedPrefsHelper.Key.INTERSTITIAL_CAP_COUNTER_SEARCH.name
        )

        // Custom crashed screen
        CaocConfig.Builder.create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT)
            .enabled(true)
            .showErrorDetails(true)
            .showRestartButton(true)
            .errorDrawable(R.mipmap.ic_launcher_round)
            .logErrorOnRestart(false)
            .trackActivities(true)
            .minTimeBetweenCrashesMs(2000)
            .restartActivity(MainActivity::class.java)
            .apply()


        // Initialize downloader
        AppSettings.initSettings(this)
        NewPipe.init(
            getDownloader(),
            Localization.getPreferredLocalization(this),
            Localization.getPreferredContentCountry(this)
        )
        Localization.init()
        configureRxJavaErrorHandler()
        initNotificationChannel()

        val intentFilter = IntentFilter("DownloadComplete")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(downloadCompleteReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(downloadCompleteReceiver, intentFilter)
        }
    }

    fun showAdIfAvailable(
        activity: Activity?,
        onShowAdCompleteListener: OnShowAdCompleteListener
    ) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with App class.
        val adConfig = SharedPrefsHelper.loadObjectPrefs(
            this, SharedPrefsHelper.Key.Open_App.name,
            AdConfig::class.java
        ) as? AdConfig
        if (adConfig != null && adConfig.Show && activity != null) {
            appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
        } else {
            onShowAdCompleteListener.onShowAdComplete()
        }
    }

    private inner class AppOpenAdManager
    /**
     * Constructor.
     */
    {
        private var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager =
            GoogleMobileAdsConsentManager.getInstance(applicationContext)
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        /**
         * Keep track of the time an app open ad is loaded to ensure you don't show an expired ad.
         */
        private var loadTime: Long = 0

        /**
         * Load an ad.
         *
         * @param context the context of the activity that loads the ad
         */
        fun loadAd(context: Context) {
            val adConfig = SharedPrefsHelper.loadObjectPrefs(
                context, SharedPrefsHelper.Key.Open_App.name,
                AdConfig::class.java
            ) as? AdConfig
            // Do not load ad if there is an unused ad or one is already loading.
            if ((adConfig != null && !adConfig.Show) || isLoadingAd || isAdAvailable) {
                return
            }
            isLoadingAd = true
            val request: AdRequest = AdRequest.Builder().build()
            adConfig?.let {
                AppOpenAd.load(
                    context,
                    it.Ad_Id,
                    request,
                    object : AppOpenAdLoadCallback() {
                        /**
                         * Called when an app open ad has loaded.
                         *
                         * @param ad the loaded app open ad.
                         */
                        override fun onAdLoaded(ad: AppOpenAd) {
                            appOpenAd = ad
                            isLoadingAd = false
                            loadTime = Date().time
                        }

                        /**
                         * Called when an app open ad has failed to load.
                         *
                         * @param loadAdError the error.
                         */
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            isLoadingAd = false
                        }
                    })
            }
        }

        /**
         * Check if ad was loaded more than n hours ago.
         */
        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        private val isAdAvailable: Boolean
            /**
             * Check if ad exists and can be shown.
             */
            get() = appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)

        /**
         * Show the ad if one isn't already showing.
         *
         * @param activity                 the activity that shows the app open ad
         * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
         */
        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener = OnShowAdCompleteListener {}
        ) {
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                return
            }

            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (!isAdAvailable) {
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
                return
            }
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                /** Called when full screen content is dismissed.  */
                override fun onAdDismissedFullScreenContent() {
                    // Set the reference to null so isAdAvailable() returns false.
                    appOpenAd = null
                    isShowingAd = false
                    onShowAdCompleteListener.onShowAdComplete()
                    if (googleMobileAdsConsentManager.canRequestAds) {
                        loadAd(activity)
                    }
                }

                /** Called when fullscreen content failed to show.  */
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    onShowAdCompleteListener.onShowAdComplete()
                    if (googleMobileAdsConsentManager.canRequestAds) {
                        loadAd(activity)
                    }
                }

                /**
                 * Called when fullscreen content is shown.
                 */
                override fun onAdShowedFullScreenContent() {}
            }
            isShowingAd = true
            appOpenAd?.show(activity)
        }
    }


    fun interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        val intentFilter = IntentFilter("DownloadComplete")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(downloadCompleteReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(downloadCompleteReceiver, intentFilter)
        }
        if (!this.appOpenAdManager.isShowingAd) {
            this.currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity

        val intentFilter = IntentFilter("DownloadComplete")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(downloadCompleteReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(downloadCompleteReceiver, intentFilter)
        }
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    @UnstableApi
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
        val adConfig = SharedPrefsHelper.loadObjectPrefs(
            this, SharedPrefsHelper.Key.Open_App.name,
            AdConfig::class.java
        ) as? AdConfig
        if (adConfig != null && adConfig.Show) {
            currentActivity?.let {
                if (it is MainActivity) {
                    appOpenAdManager.showAdIfAvailable(it)
                }
            }
        }
    }

    private fun queryAdConfigValueListener() {
        dataRef = mDatabase?.child("Ads")
        val query: Query? = dataRef?.orderByKey()
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (child in dataSnapshot.children) {
                    val adConfig = child.getValue(AdConfig::class.java)
                    if (adConfig != null) {
                        SharedPrefsHelper.saveObjectPrefs(this@App, child.key, adConfig)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        query?.addValueEventListener(valueEventListener as ValueEventListener)
    }

    private fun getDownloader(): Downloader {
        val downloader: DownloaderImpl = DownloaderImpl.init(null)
        setCookiesToDownloader(downloader)
        return downloader
    }

    private fun setCookiesToDownloader(downloader: DownloaderImpl) {
        downloader.updateYoutubeRestrictedModeCookies(applicationContext)
    }

    private fun configureRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler(object : Consumer<Throwable> {
            override fun accept(throwable: Throwable) {
                var newThrowable = throwable
                if (throwable is UndeliverableException) {
                    // As UndeliverableException is a wrapper, get the cause of it to get the "real" exception
                    newThrowable = throwable.cause!!
                }
                val errors: List<Throwable> = if (newThrowable is CompositeException) {
                    newThrowable.exceptions
                } else {
                    listOf(newThrowable)
                }

                for (error in errors) {
                    if (isThrowableIgnored(error)) return
                    if (isThrowableCritical(error)) {
                        reportException(error)
                        return
                    }
                }
            }

            private fun isThrowableIgnored(throwable: Throwable): Boolean {
                // Don't crash the application over a simple network problem
                return ExtractorHelper.hasAssignableCauseThrowable(
                    throwable,
                    IOException::class.java,
                    SocketException::class.java,  // network api cancellation
                    InterruptedException::class.java,
                    InterruptedIOException::class.java
                ) // blocking code disposed
            }

            private fun isThrowableCritical(throwable: Throwable): Boolean {
                // Though these exceptions cannot be ignored
                return ExtractorHelper.hasAssignableCauseThrowable(
                    throwable,
                    NullPointerException::class.java,
                    IllegalArgumentException::class.java,  // bug in app
                    OnErrorNotImplementedException::class.java,
                    MissingBackpressureException::class.java,
                    IllegalStateException::class.java
                ) // bug in operator
            }

            private fun reportException(throwable: Throwable) {
                // Throw uncaught exception that will trigger the report system
                Objects.requireNonNull(Thread.currentThread().uncaughtExceptionHandler)
                    .uncaughtException(
                        Thread.currentThread(), throwable
                    )
            }
        })
    }

    private fun initNotificationChannel() {
        val downloadChannel = NotificationChannelCompat.Builder(
            getString(R.string.download),
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        ).setName(getString(R.string.download)).build()

        val notificationChannelCompats = listOf(downloadChannel)

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.createNotificationChannelsCompat(notificationChannelCompats)
    }
}
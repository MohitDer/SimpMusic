package com.maxrave.simpmusic.downloader.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.preference.PreferenceManager;

import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.downloader.helper.DownloadMission;
import com.maxrave.simpmusic.downloader.helper.MissionRecoveryInfo;
import com.maxrave.simpmusic.downloader.io.StoredDirectoryHelper;
import com.maxrave.simpmusic.downloader.io.StoredFileHelper;
import com.maxrave.simpmusic.downloader.processor.PostProcessing;
import com.maxrave.simpmusic.downloader.service.DownloadManager.NetworkState;
import com.maxrave.simpmusic.ui.fragment.download.DownloadActivity;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DownloadManagerService extends Service {

    private static final String TAG = "DownloadManagerService";

    public static final int MESSAGE_RUNNING = 0;
    public static final int MESSAGE_PAUSED = 1;
    public static final int MESSAGE_FINISHED = 2;
    public static final int MESSAGE_ERROR = 3;
    public static final int MESSAGE_DELETED = 4;

    private static final int FOREGROUND_NOTIFICATION_ID = 1000;
    private static final int DOWNLOADS_NOTIFICATION_ID = 1001;

    private static final String EXTRA_URLS = "DownloadManagerService.extra.urls";
    private static final String EXTRA_KIND = "DownloadManagerService.extra.kind";
    private static final String EXTRA_THREADS = "DownloadManagerService.extra.threads";
    private static final String EXTRA_POSTPROCESSING_NAME = "DownloadManagerService.extra.postprocessingName";
    private static final String EXTRA_POSTPROCESSING_ARGS = "DownloadManagerService.extra.postprocessingArgs";
    private static final String EXTRA_SOURCE = "DownloadManagerService.extra.source";
    private static final String EXTRA_NEAR_LENGTH = "DownloadManagerService.extra.nearLength";
    private static final String EXTRA_PATH = "DownloadManagerService.extra.storagePath";
    private static final String EXTRA_PARENT_PATH = "DownloadManagerService.extra.storageParentPath";
    private static final String EXTRA_STORAGE_TAG = "DownloadManagerService.extra.storageTag";
    private static final String EXTRA_RECOVERY_INFO = "DownloadManagerService.extra.recoveryInfo";

    private static final String ACTION_RESET_DOWNLOAD_FINISHED = "com.musicdownloader.musicplayer" + ".reset_download_finished";
    private static final String ACTION_OPEN_DOWNLOADS_FINISHED = "com.musicdownloader.musicplayer" + ".open_downloads_finished";

    private DownloadManagerBinder mBinder;
    private DownloadManager mManager;
    private Notification mNotification;
    private Handler mHandler;
    private boolean mForeground = false;
    private NotificationManagerCompat mNotificationManager = null;
    private boolean mDownloadNotificationEnable = true;

    private static final String CHANNEL_ID = "download_notifications_channel";

    private int downloadDoneCount = 0;
    private NotificationCompat.Builder downloadDoneNotification = null;
    private StringBuilder downloadDoneList = null;

    private final ArrayList<Callback> mEchoObservers = new ArrayList<>(1);

    private ConnectivityManager mConnectivityManager;
    private ConnectivityManager.NetworkCallback mNetworkStateListenerL = null;

    private SharedPreferences mPrefs = null;
    private final OnSharedPreferenceChangeListener mPrefChangeListener = this::handlePreferenceChange;

    private boolean mLockAcquired = false;
    private LockManager mLock = null;

    private int downloadFailedNotificationID = DOWNLOADS_NOTIFICATION_ID + 1;
    private NotificationCompat.Builder downloadFailedNotification = null;
    private final SparseArray<DownloadMission> mFailedDownloads = new SparseArray<>(5);

    private Bitmap icLauncher;
    private Bitmap icDownloadDone;
    private Bitmap icDownloadFailed;

    private PendingIntent mOpenDownloadList;

    /**
     * notify media scanner on downloaded media file ...
     *
     * @param file the downloaded file uri
     */
    private void notifyMediaScanner(Uri file) {
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, file).setPackage(getPackageName()));
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onCreate() {
        super.onCreate();

        mBinder = new DownloadManagerBinder();
        mHandler = new Handler(this::handleMessage);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mManager = new DownloadManager(this, mHandler, loadMainVideoStorage(), loadMainAudioStorage());

        Intent openDownloadListIntent = new Intent(this, DownloadActivity.class)
                .setAction(Intent.ACTION_MAIN);

        mOpenDownloadList = PendingIntent.getActivity(this, 0,
                openDownloadListIntent,
                PendingIntent.FLAG_IMMUTABLE);

        icLauncher = BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.download))
                .setContentIntent(mOpenDownloadList)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setLargeIcon(icLauncher)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentTitle(getString(R.string.msg_running))
                .setContentText(getString(R.string.msg_running_detail));

        mNotification = builder.build();

        mNotificationManager = NotificationManagerCompat.from(this);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mNetworkStateListenerL = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NotNull Network network) {
                handleConnectivityState(false);
            }

            @Override
            public void onLost(@NotNull Network network) {
                handleConnectivityState(false);
            }
        };
        mConnectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), mNetworkStateListenerL);

        mPrefs.registerOnSharedPreferenceChangeListener(mPrefChangeListener);

        handlePreferenceChange(mPrefs, getString(R.string.downloads_cross_network));
        handlePreferenceChange(mPrefs, getString(R.string.downloads_maximum_retry));

        mLock = new LockManager(this);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        if (intent == null) return START_NOT_STICKY;

        Log.i(TAG, "Got intent: " + intent);
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(Intent.ACTION_RUN)) {
                mHandler.post(() -> startMission(intent));
            } else if (downloadDoneNotification != null) {
                if (action.equals(ACTION_RESET_DOWNLOAD_FINISHED) || action.equals(ACTION_OPEN_DOWNLOADS_FINISHED)) {
                    downloadDoneCount = 0;
                    downloadDoneList.setLength(0);
                }
                if (action.equals(ACTION_OPEN_DOWNLOADS_FINISHED)) {
                    startActivity(new Intent(this, DownloadActivity.class)
                            .setAction(Intent.ACTION_MAIN)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    );
                }
                return START_NOT_STICKY;
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForeground(true);

        if (mNotificationManager != null && downloadDoneNotification != null) {
            downloadDoneNotification.setDeleteIntent(null);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                mNotificationManager.notify(DOWNLOADS_NOTIFICATION_ID, downloadDoneNotification.build());
            }
        }

        manageLock(false);

        mConnectivityManager.unregisterNetworkCallback(mNetworkStateListenerL);
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefChangeListener);

        if (icDownloadDone != null) icDownloadDone.recycle();
        if (icDownloadFailed != null) icDownloadFailed.recycle();
        if (icLauncher != null) icLauncher.recycle();

        mHandler = null;
        mManager.pauseAllMissions(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private boolean handleMessage(@NonNull Message msg) {
        if (mHandler == null) return true;

        DownloadMission mission = (DownloadMission) msg.obj;

        switch (msg.what) {
            case MESSAGE_FINISHED:
                Intent broadcastIntent = new Intent("DownloadComplete");
                sendBroadcast(broadcastIntent);
                notifyMediaScanner(mission.storage.getUri());
                notifyFinishedDownload(mission.storage.getName());
                mManager.setFinished(mission);
                handleConnectivityState(false);
                updateForegroundState(mManager.runMissions());
                break;
            case MESSAGE_RUNNING:
                updateForegroundState(true);
                break;
            case MESSAGE_ERROR:
                notifyFailedDownload(mission);
                handleConnectivityState(false);
                updateForegroundState(mManager.runMissions());
                break;
            case MESSAGE_PAUSED:
                updateForegroundState(mManager.getRunningMissionsCount() > 0);
                break;
        }

        if (msg.what != MESSAGE_ERROR)
            mFailedDownloads.delete(mFailedDownloads.indexOfValue(mission));

        for (Callback observer : mEchoObservers)
            observer.handleMessage(msg);

        return true;
    }

    private void handleConnectivityState(boolean updateOnly) {
        NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        NetworkState status;

        if (info == null) {
            status = NetworkState.Unavailable;
            Log.i(TAG, "Active network [connectivity is unavailable]");
        } else {
            boolean connected = info.isConnected();
            boolean metered = mConnectivityManager.isActiveNetworkMetered();

            if (connected)
                status = metered ? NetworkState.MeteredOperating : NetworkState.Operating;
            else
                status = NetworkState.Unavailable;

            Log.i(TAG, "Active network [connected=" + connected + " metered=" + metered + "] " + info.toString());
        }

        if (mManager == null) return;// avoid race-conditions while the service is starting
        mManager.handleConnectivityState(status, updateOnly);
    }

    private void handlePreferenceChange(SharedPreferences prefs, @NonNull String key) {
        if (key.equals(getString(R.string.downloads_maximum_retry))) {
            try {
                String value = prefs.getString(key, getString(R.string.downloads_maximum_retry_default));
                mManager.mPrefMaxRetry = Integer.parseInt(value);
            } catch (Exception e) {
                mManager.mPrefMaxRetry = 0;
            }
            mManager.updateMaximumAttempts();
        } else if (key.equals(getString(R.string.downloads_cross_network))) {
            mManager.mPrefMeteredDownloads = prefs.getBoolean(key, false);
        } else if (key.equals(getString(R.string.download_path_video_key))) {
            mManager.mMainStorageVideo = loadMainVideoStorage();
        } else if (key.equals(getString(R.string.download_path_audio_key))) {
            mManager.mMainStorageAudio = loadMainAudioStorage();
        }
    }

    public void updateForegroundState(boolean state) {
        if (state == mForeground) return;

        if (state) {
            startForeground(FOREGROUND_NOTIFICATION_ID, mNotification);
        } else {
            stopForeground(true);
        }

        manageLock(state);

        mForeground = state;
    }

    /**
     * Start a new download mission
     *
     * @param context      the activity context
     * @param urls         array of urls to download
     * @param storage      where the file is saved
     * @param kind         type of file (a: audio  v: video  s: subtitle ?: file-extension defined)
     * @param threads      the number of threads maximal used to download chunks of the file.
     * @param psName       the name of the required post-processing algorithm, or {@code null} to ignore.
     * @param source       source url of the resource
     * @param psArgs       the arguments for the post-processing algorithm.
     * @param nearLength   the approximated final length of the file
     * @param recoveryInfo array of MissionRecoveryInfo, in case is required recover the download
     */
    public static void startMission(Context context, String[] urls, StoredFileHelper storage,
                                    char kind, int threads, String source, String psName,
                                    String[] psArgs, long nearLength, MissionRecoveryInfo[] recoveryInfo) {
        Intent intent = new Intent(context, DownloadManagerService.class);
        intent.setPackage(context.getPackageName());
        intent.setAction(Intent.ACTION_RUN);
        intent.putExtra(EXTRA_URLS, urls);
        intent.putExtra(EXTRA_KIND, kind);
        intent.putExtra(EXTRA_THREADS, threads);
        intent.putExtra(EXTRA_SOURCE, source);
        intent.putExtra(EXTRA_POSTPROCESSING_NAME, psName);
        intent.putExtra(EXTRA_POSTPROCESSING_ARGS, psArgs);
        intent.putExtra(EXTRA_NEAR_LENGTH, nearLength);
        intent.putExtra(EXTRA_RECOVERY_INFO, recoveryInfo);

        intent.putExtra(EXTRA_PARENT_PATH, storage.getParentUri());
        intent.putExtra(EXTRA_PATH, storage.getUri());
        intent.putExtra(EXTRA_STORAGE_TAG, storage.getTag());

        context.startService(intent);
    }

    private void startMission(Intent intent) {
        String[] urls = intent.getStringArrayExtra(EXTRA_URLS);
        Uri path = intent.getParcelableExtra(EXTRA_PATH);
        Uri parentPath = intent.getParcelableExtra(EXTRA_PARENT_PATH);
        int threads = intent.getIntExtra(EXTRA_THREADS, 20);
        char kind = intent.getCharExtra(EXTRA_KIND, '?');
        String psName = intent.getStringExtra(EXTRA_POSTPROCESSING_NAME);
        String[] psArgs = intent.getStringArrayExtra(EXTRA_POSTPROCESSING_ARGS);
        String source = intent.getStringExtra(EXTRA_SOURCE);
        long nearLength = intent.getLongExtra(EXTRA_NEAR_LENGTH, 0);
        String tag = intent.getStringExtra(EXTRA_STORAGE_TAG);
        Parcelable[] parcelRecovery = intent.getParcelableArrayExtra(EXTRA_RECOVERY_INFO);

        StoredFileHelper storage;
        try {
            storage = new StoredFileHelper(this, parentPath, path, tag);
        } catch (IOException e) {
            throw new RuntimeException(e);// this never should happen
        }

        PostProcessing ps;
        if (psName == null)
            ps = null;
        else
            ps = PostProcessing.getAlgorithm(psName, psArgs);

        MissionRecoveryInfo[] recovery = new MissionRecoveryInfo[parcelRecovery.length];
        for (int i = 0; i < parcelRecovery.length; i++)
            recovery[i] = (MissionRecoveryInfo) parcelRecovery[i];

        final DownloadMission mission = new DownloadMission(urls, storage, kind, ps);
        mission.threadCount = threads;
        mission.source = source;
        mission.nearLength = nearLength;
        mission.recoveryInfo = recovery;

        if (ps != null)
            ps.setTemporalDir(DownloadManager.pickAvailableTemporalDir(this));

        handleConnectivityState(true);// first check the actual network status

        mManager.startMission(mission);
    }

    @SuppressLint({"MissingPermission", "ObsoleteSdkInt"})
    public void notifyFinishedDownload(String name) {

        Toast.makeText(getApplicationContext(), name+" Download Complete", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getApplicationContext(), DownloadActivity.class);
        intent.putExtra("Frag","Download");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        @SuppressLint("LaunchActivityFromNotification") NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(name)
                .setContentText("Download Complete")
                .setDeleteIntent(pendingIntent)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence NotificationName = "ProTube"; // The name of the channel
            int importance = NotificationManager.IMPORTANCE_MAX;
            @SuppressLint("WrongConstant") NotificationChannel channel = new NotificationChannel(CHANNEL_ID, NotificationName, importance);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            NotificationManagerCompat notificationManager1 = NotificationManagerCompat.from(getApplicationContext());
            notificationManager1.notify(DOWNLOADS_NOTIFICATION_ID, builder.build());
        }
    }
    public void notifyFailedDownload(DownloadMission mission) {
        if (!mDownloadNotificationEnable || mFailedDownloads.indexOfValue(mission) >= 0) return;

        int id = downloadFailedNotificationID++;
        mFailedDownloads.put(id, mission);

        if (downloadFailedNotification == null) {
            icDownloadFailed = BitmapFactory.decodeResource(this.getResources(), android.R.drawable.stat_sys_warning);
            downloadFailedNotification = new NotificationCompat.Builder(this, getString(R.string.download))
                    .setAutoCancel(true)
                    .setLargeIcon(icDownloadFailed)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentIntent(mOpenDownloadList);
        }

        downloadFailedNotification.setContentTitle(getString(R.string.download_failed));
        downloadFailedNotification.setContentText(mission.storage.getName());
        downloadFailedNotification.setStyle(new NotificationCompat.BigTextStyle().bigText(mission.storage.getName()));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            mNotificationManager.notify(id, downloadFailedNotification.build());
        }
    }

    private PendingIntent makePendingIntent(String action) {
        Intent intent = new Intent(this, DownloadManagerService.class)
                .setPackage(getPackageName())
                .setAction(action);
        return PendingIntent.getService(this, intent.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private void manageLock(boolean acquire) {
        if (acquire == mLockAcquired) return;

        if (acquire)
            mLock.acquireWifiAndCpu();
        else
            mLock.releaseWifiAndCpu();

        mLockAcquired = acquire;
    }

    private StoredDirectoryHelper loadMainVideoStorage() {
        return loadMainStorage(R.string.download_path_video_key, DownloadManager.TAG_VIDEO);
    }

    private StoredDirectoryHelper loadMainAudioStorage() {
        return loadMainStorage(R.string.download_path_audio_key, DownloadManager.TAG_AUDIO);
    }

    private StoredDirectoryHelper loadMainStorage(@StringRes int prefKey, String tag) {
        String path = mPrefs.getString(getString(prefKey), null);

        if (path == null || path.isEmpty()) return null;

        if (path.charAt(0) == File.separatorChar) {
            Log.i(TAG, "Old save path style present: " + path);
            path = "";
            mPrefs.edit().putString(getString(prefKey), "").apply();
        }

        try {
            return new StoredDirectoryHelper(this, Uri.parse(path), tag);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load the storage of " + tag + " from " + path, e);
        }

        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Wrappers for DownloadManager
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public class DownloadManagerBinder extends Binder {
        public DownloadManager getDownloadManager() {
            return mManager;
        }

        @Nullable
        public StoredDirectoryHelper getMainStorageVideo() {
            return mManager.mMainStorageVideo;
        }

        @Nullable
        public StoredDirectoryHelper getMainStorageAudio() {
            return mManager.mMainStorageAudio;
        }

        public void addMissionEventListener(Callback handler) {
            mEchoObservers.add(handler);
        }

        public void removeMissionEventListener(Callback handler) {
            mEchoObservers.remove(handler);
        }

        public void clearDownloadNotifications() {
            if (mNotificationManager == null) return;
            if (downloadDoneNotification != null) {
                mNotificationManager.cancel(DOWNLOADS_NOTIFICATION_ID);
                downloadDoneList.setLength(0);
                downloadDoneCount = 0;
            }
            if (downloadFailedNotification != null) {
                for (; downloadFailedNotificationID > DOWNLOADS_NOTIFICATION_ID; downloadFailedNotificationID--) {
                    mNotificationManager.cancel(downloadFailedNotificationID);
                }
                mFailedDownloads.clear();
                downloadFailedNotificationID++;
            }
        }

        public void enableNotifications(boolean enable) {
            mDownloadNotificationEnable = enable;
        }
    }
}

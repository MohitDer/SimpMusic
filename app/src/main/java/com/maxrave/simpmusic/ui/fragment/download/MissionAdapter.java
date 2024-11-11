package com.maxrave.simpmusic.ui.fragment.download;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_GRANT_PREFIX_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static androidx.core.content.ContextCompat.startActivity;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_CONNECT_HOST;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_FILE_CREATION;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_HTTP_NO_CONTENT;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_INSUFFICIENT_STORAGE;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_NOTHING;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_PATH_CREATION;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_PERMISSION_DENIED;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_POSTPROCESSING;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_POSTPROCESSING_HOLD;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_POSTPROCESSING_STOPPED;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_PROGRESS_LOST;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_RESOURCE_GONE;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_SSL_EXCEPTION;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_TIMEOUT;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_UNKNOWN_EXCEPTION;
import static com.maxrave.simpmusic.downloader.helper.DownloadMission.ERROR_UNKNOWN_HOST;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.ads.admob.AdMobInterstitialAdSong;
import com.maxrave.simpmusic.downloader.ExtractorHelper;
import com.maxrave.simpmusic.downloader.common.Deleter;
import com.maxrave.simpmusic.downloader.common.ProgressDrawable;
import com.maxrave.simpmusic.downloader.helper.DownloadMission;
import com.maxrave.simpmusic.downloader.helper.FinishedMission;
import com.maxrave.simpmusic.downloader.helper.Mission;
import com.maxrave.simpmusic.downloader.io.StoredFileHelper;
import com.maxrave.simpmusic.downloader.service.DownloadManager;
import com.maxrave.simpmusic.downloader.service.DownloadManagerService;
import com.maxrave.simpmusic.downloader.util.ImageUtils;
import com.maxrave.simpmusic.downloader.util.Utility;
import com.maxrave.simpmusic.ui.fragment.download.player.MusicPlayerActivity;
import com.maxrave.simpmusic.ui.fragment.download.player.VideoPlayerActivity;
import com.maxrave.simpmusic.ui.fragment.download.songs.FileUtil;
import com.maxrave.simpmusic.ui.fragment.download.songs.Song;
import com.maxrave.simpmusic.ui.fragment.download.videos.Video;

import org.schabi.newpipe.extractor.ServiceList;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MissionAdapter extends Adapter<ViewHolder> implements Handler.Callback {

    private static final String UNDEFINED_PROGRESS = "--.--%";
    private static final String DEFAULT_MIME_TYPE = "*/*";
    private static final String UNDEFINED_ETA = "--:--";

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final DownloadManager mDownloadManager;
    private final Deleter mDeleter;
    private final int mLayout;
    private final DownloadManager.MissionIterator mIterator;
    private final ArrayList<ViewHolderItem> mPendingDownloadsItems = new ArrayList<>();
    private final Handler mHandler;
    private MenuItem mClear;
    private MenuItem mStartButton;
    private MenuItem mPauseButton;
    private RecoverHelper mRecover;
    private final View mView;
    private final ArrayList<Mission> mHidden;
    private Snackbar mSnackbar;

    private final Runnable rUpdater = this::updater;
    private final Runnable rDelete = this::deleteFinishedDownloads;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public MissionAdapter(Context context, @NonNull DownloadManager downloadManager, View root) {
        mContext = context;
        mDownloadManager = downloadManager;

        AdMobInterstitialAdSong.getInstance().init((Activity) mContext);

        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayout = R.layout.mission_item;

        mHandler = new Handler(context.getMainLooper());

        mIterator = downloadManager.getIterator();

        mDeleter = new Deleter(root, mContext, this, mDownloadManager, mIterator, mHandler);

        mView = root;

        mHidden = new ArrayList<>();

        onResume();


    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case DownloadManager.SPECIAL_PENDING:
            case DownloadManager.SPECIAL_FINISHED:
                return new ViewHolderHeader(mInflater.inflate(R.layout.missions_header, parent, false));
        }

        return new ViewHolderItem(mInflater.inflate(mLayout, parent, false));
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder view) {
        super.onViewRecycled(view);

        if (view instanceof ViewHolderHeader)
            return;
        ViewHolderItem h = (ViewHolderItem) view;

        if (h.item.mission instanceof DownloadMission) {
            mPendingDownloadsItems.remove(h);
            if (mPendingDownloadsItems.isEmpty()) {
                checkMasterButtonsVisibility();
            }
        }

        h.popupMenu.dismiss();
        h.item = null;
        h.resetSpeedMeasure();
    }

    @Override
    @SuppressLint({"SetTextI18n", "CheckResult"})
    public void onBindViewHolder(@NonNull ViewHolder view, @SuppressLint("RecyclerView") int pos) {
        DownloadManager.MissionItem item = mIterator.getItem(pos);

        if (view instanceof ViewHolderHeader) {
            if (item.special == DownloadManager.SPECIAL_NOTHING)
                return;
            int str;
            if (item.special == DownloadManager.SPECIAL_PENDING) {
                str = R.string.missions_header_pending;
            } else {
                str = R.string.missions_header_finished;
                if (mClear != null)
                    mClear.setVisible(true);
            }

            ((ViewHolderHeader) view).header.setText(str);
            return;
        }

        ViewHolderItem h = (ViewHolderItem) view;
        h.item = item;

        Utility.FileType type = Utility.getFileType(item.mission.kind, item.mission.storage.getName());

        h.mediaIcon.setImageResource(Utility.getIconForFileType(type));

        if (Utility.FileType.VIDEO.equals(type)) {
            Glide.with(mContext)
                    .asBitmap()
                    .load(item.mission.storage.getUri())
                    .apply(new RequestOptions().placeholder(R.drawable.holder_video).centerCrop())
                    .into(h.icon);
        } else {
            compositeDisposable.add(ExtractorHelper.getStreamInfo(ServiceList.YouTube.getServiceId(), item.mission.source)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(
                            // onNext
                            streamInfo -> {
                                if (mContext != null) {
                                    ImageUtils.loadThumbnail(mContext, h.icon, streamInfo.getThumbnails());
                                }
                            },
                            // onError
                            throwable -> {
                            }));
        }

        h.name.setText(item.mission.storage.getName());

        h.progress.setColors(Utility.getBackgroundForFileType(mContext), Utility.getForegroundForFileType(mContext));

        if (h.item.mission instanceof DownloadMission) {
            DownloadMission mission = (DownloadMission) item.mission;
//            String length = Utility.formatBytes(mission.getLength());
//            if (mission.running && !mission.isPsRunning())
//                length += " --.-- kB/s";
//            h.size.setText(length);
            h.size.setText(Utility.formatBytes(mission.getLength()));
            h.pause.setTitle(mission.unknownLength ? R.string.stop : R.string.pause);
            updateProgress(h);
            mPendingDownloadsItems.add(h);
        } else {
            h.progress.setMarquee(false);
            h.status.setText("100%");
            h.progress.setProgress(1f);
            h.size.setText(Utility.formatBytes(item.mission.length));
        }
    }

    @Override
    public int getItemCount() {
        return mIterator.getOldListSize();
    }

    @Override
    public int getItemViewType(int position) {
        return mIterator.getSpecialAtItem(position);
    }

    @SuppressLint("DefaultLocale")
    private void updateProgress(ViewHolderItem h) {
        if (h == null || h.item == null || h.item.mission instanceof FinishedMission)
            return;

        DownloadMission mission = (DownloadMission) h.item.mission;
        double done = mission.done;
        long length = mission.getLength();
        long now = System.currentTimeMillis();
        boolean hasError = mission.errCode != ERROR_NOTHING;

        // hide on error
        // show if current resource length is not fetched
        // show if length is unknown
        h.progress.setMarquee(mission.isRecovering() || !hasError && (!mission.isInitialized() || mission.unknownLength));

        double progress;
        if (mission.unknownLength) {
            progress = Double.NaN;
            h.progress.setProgress(0f);
        } else {
            progress = done / length;
        }

        if (hasError) {
            h.progress.setProgress(isNotFinite(progress) ? 1d : progress);
            h.status.setText(R.string.msg_error);
        } else if (isNotFinite(progress)) {
            h.status.setText(UNDEFINED_PROGRESS);
        } else {
            h.status.setText(String.format("%d%%", (int) (progress * 100)));
            h.progress.setProgress(progress);
        }

        @StringRes int state;
        String sizeStr = Utility.formatBytes(length).concat("  ");

        if (mission.isPsFailed() || mission.errCode == ERROR_POSTPROCESSING_HOLD) {
            h.size.setText(sizeStr);
            return;
        } else if (!mission.running) {
            state = mission.enqueued ? R.string.queued : R.string.paused;
        } else if (mission.isPsRunning()) {
            state = R.string.post_processing;
        } else if (mission.isRecovering()) {
            state = R.string.recovering;
        } else {
            state = 0;
        }

        if (state != 0) {
            // update state without download speed
            h.size.setText(sizeStr.concat("(").concat(mContext.getString(state)).concat(")"));
            h.resetSpeedMeasure();
            return;
        }

        if (h.lastTimestamp < 0) {
            h.size.setText(sizeStr);
            h.lastTimestamp = now;
            h.lastDone = done;
            return;
        }

        long deltaTime = now - h.lastTimestamp;
        double deltaDone = done - h.lastDone;

        if (h.lastDone > done) {
            h.lastDone = done;
            h.size.setText(sizeStr);
            return;
        }

        if (deltaDone > 0 && deltaTime > 0) {
            float speed = (float) ((deltaDone * 1000d) / deltaTime);
            float averageSpeed = speed;

            if (h.lastSpeedIdx < 0) {
                Arrays.fill(h.lastSpeed, speed);
                h.lastSpeedIdx = 0;
            } else {
                for (int i = 0; i < h.lastSpeed.length; i++) {
                    averageSpeed += h.lastSpeed[i];
                }
                averageSpeed /= h.lastSpeed.length + 1f;
            }

            String speedStr = Utility.formatSpeed(averageSpeed);
//            String etaStr;
//            if (mission.unknownLength) {
//                etaStr = "";
//            } else {
//                long eta = (long) Math.ceil((length - done) / averageSpeed);
//                etaStr = Utility.formatBytes((long) done) + "/" + Utility.stringifySeconds(eta) + "  ";
//            }
//			h.size.setText(sizeStr.concat(etaStr).concat(speedStr));
            h.size.setText(sizeStr.concat(speedStr));

            h.lastTimestamp = now;
            h.lastDone = done;
            h.lastSpeed[h.lastSpeedIdx++] = speed;

            if (h.lastSpeedIdx >= h.lastSpeed.length)
                h.lastSpeedIdx = 0;
        }
    }

    private void viewWithFileProvider(Mission mission) {
        if (checkInvalidFile(mission))
            return;

        String mimeType = resolveMimeType(mission);
        Uri uri = resolveShareableUri(mission);

        Intent intent = new Intent();
        intent.setPackage(mContext.getPackageName());
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(FLAG_GRANT_PREFIX_URI_PERMISSION);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);

        if (intent.resolveActivity(mContext.getPackageManager()) != null) {
            mContext.startActivity(intent);
        } else {
            Toast.makeText(mContext, R.string.toast_no_player, Toast.LENGTH_LONG).show();
        }
    }

    private void shareFile(Mission mission) {
        if (mContext != null){
            if (checkInvalidFile(mission))
                return;
            MediaScannerConnection.scanFile(
                    mContext,
                    new String[]{mission.storage.getUri().getPath()},
                    null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {

                            if (mission.storage.getName().contains("mp3") &&mission.storage.getName().contains("m4a") ) {
                                shareAudioWithText(path);
                            } else {
                                shareMediaFile(mission, uri);
                            }

                        }
                    }
            );
        }

    }

    private void shareMediaFile(Mission mission, Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, mission.storage.getName());
        shareIntent.putExtra(Intent.EXTRA_TITLE, mission.storage.getName());
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        String description = "Check out this video I downloaded using the Audio Downloader app! Download the app here: \n https://play.google.com/store/apps/details?id=" + mContext.getPackageName();
        shareIntent.setType("video/*");
        shareIntent.putExtra(Intent.EXTRA_TEXT, description);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        mContext.startActivity(Intent.createChooser(shareIntent, description));
    }

    private void shareAudioWithText(String uri) {
        File audioFile = new File(uri);
        if (audioFile.exists()) {
            String description = "Check out this Audio I downloaded using the Audio Downloader app! Download the app here: \n https://play.google.com/store/apps/details?id=" + mContext.getPackageName();
            Uri audioUri = FileProvider.getUriForFile(mContext, "com.musicdownloader.musicplayer.fileprovider", audioFile);
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, description);
            shareIntent.putExtra(Intent.EXTRA_STREAM, audioUri);
            shareIntent.setType("audio/*");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Use chooser to ensure user selects the app to share with
            Intent chooser = Intent.createChooser(shareIntent, "Share audio using");

            // Verify that the intent will resolve to at least one activity
            if (shareIntent.resolveActivity(mContext.getPackageManager()) != null) {
                mContext.startActivity(chooser);
            } else {
                Toast.makeText(mContext, "No app available to share audio.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mContext, "Audio file does not exist.", Toast.LENGTH_SHORT).show();
        }
    }

    public static String getFileType(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            String mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);

            if (mimeType != null) {
                if (mimeType.startsWith("video")) {
                    return "video";
                } else if (mimeType.startsWith("audio")) {
                    return "audio";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return "unknown";
    }

    private Uri resolveShareableUri(Mission mission) {
        if (mission.storage.isDirect()) {
            return FileProvider.getUriForFile(mContext, "com.musicdownloader.musicplayer" + ".provider", new File(URI.create(mission.storage.getUri().toString())));
        } else {
            return mission.storage.getUri();
        }
    }

    private static String resolveMimeType(@NonNull Mission mission) {
        String mimeType;

        if (!mission.storage.isInvalid()) {
            mimeType = mission.storage.getType();
            if (mimeType != null && mimeType.length() > 0 && !mimeType.equals(StoredFileHelper.DEFAULT_MIME))
                return mimeType;
        }

        String ext = Utility.getFileExtension(mission.storage.getName());
        if (ext == null)
            return DEFAULT_MIME_TYPE;

        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.substring(1));

        return mimeType == null ? DEFAULT_MIME_TYPE : mimeType;
    }

    private boolean checkInvalidFile(@NonNull Mission mission) {
        if (mission.storage.existsAsFile())
            return false;

        Toast.makeText(mContext, R.string.missing_file, Toast.LENGTH_SHORT).show();
        return true;
    }

    private ViewHolderItem getViewHolder(Object mission) {
        for (ViewHolderItem h : mPendingDownloadsItems) {
            if (h.item.mission == mission)
                return h;
        }
        return null;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (mStartButton != null && mPauseButton != null) {
            checkMasterButtonsVisibility();
        }

        switch (msg.what) {
            case DownloadManagerService.MESSAGE_ERROR:
            case DownloadManagerService.MESSAGE_FINISHED:
            case DownloadManagerService.MESSAGE_DELETED:
            case DownloadManagerService.MESSAGE_PAUSED:
                break;
            default:
                return false;
        }

        ViewHolderItem h = getViewHolder(msg.obj);
        if (h == null)
            return false;

        switch (msg.what) {
            case DownloadManagerService.MESSAGE_FINISHED:
            case DownloadManagerService.MESSAGE_DELETED:
                // DownloadManager should mark the download as finished
                applyChanges();
                return true;
        }

        updateProgress(h);
        return true;
    }

    private void showError(@NonNull DownloadMission mission) {
        @StringRes int msg = R.string.something_went_wrong;
        String msgEx = null;

        switch (mission.errCode) {
            case 416:
                msg = R.string.error_http_unsupported_range;
                break;
            case 404:
                msg = R.string.error_http_not_found;
                break;
            case ERROR_NOTHING:
                return;
            case ERROR_FILE_CREATION:
                msg = R.string.error_file_creation;
                break;
            case ERROR_HTTP_NO_CONTENT:
                msg = R.string.error_http_no_content;
                break;
            case ERROR_PATH_CREATION:
                msg = R.string.error_path_creation;
                break;
            case ERROR_PERMISSION_DENIED:
                msg = R.string.permission_denied;
                break;
            case ERROR_SSL_EXCEPTION:
                msg = R.string.error_ssl_exception;
                break;
            case ERROR_UNKNOWN_HOST:
                msg = R.string.error_unknown_host;
                break;
            case ERROR_CONNECT_HOST:
                msg = R.string.error_connect_host;
                break;
            case ERROR_POSTPROCESSING_STOPPED:
                msg = R.string.error_postprocessing_stopped;
                break;
            case ERROR_POSTPROCESSING:
            case ERROR_POSTPROCESSING_HOLD:
                msg = R.string.error_postprocessing_failed;
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                return;
            case ERROR_INSUFFICIENT_STORAGE:
                msg = R.string.error_insufficient_storage;
                break;
            case ERROR_UNKNOWN_EXCEPTION:
                if (mission.errObject != null) {
                    Toast.makeText(mContext, R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    msg = R.string.msg_error;
                    break;
                }
            case ERROR_PROGRESS_LOST:
                msg = R.string.error_progress_lost;
                break;
            case ERROR_TIMEOUT:
                msg = R.string.error_timeout;
                break;
            case ERROR_RESOURCE_GONE:
                msg = R.string.error_download_resource_gone;
                break;
            default:
                if (mission.errCode >= 100 && mission.errCode < 600) {
                    msgEx = "HTTP " + mission.errCode;
                } else if (mission.errObject == null) {
                    msgEx = "(not_decelerated_error_code)";
                } else {
                    Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
        }

        Toast.makeText(mContext, msgEx != null ? msgEx : mContext.getString(msg), Toast.LENGTH_SHORT).show();
    }

    public void clearFinishedDownloads(boolean delete) {
        if (delete && mIterator.hasFinishedMissions() && mHidden.isEmpty()) {
            for (int i = 0; i < mIterator.getOldListSize(); i++) {
                FinishedMission mission = mIterator.getItem(i).mission instanceof FinishedMission ? (FinishedMission) mIterator.getItem(i).mission : null;
                if (mission != null) {
                    mIterator.hide(mission);
                    mHidden.add(mission);
                }
            }
            applyChanges();

            String msg = mContext.getString(R.string.deleted_downloads);
            mSnackbar = Snackbar.make(mView, msg, Snackbar.LENGTH_INDEFINITE);
            mSnackbar.setAction(R.string.undo, s -> {
                Iterator<Mission> i = mHidden.iterator();
                while (i.hasNext()) {
                    mIterator.unHide(i.next());
                    i.remove();
                }
                applyChanges();
                mHandler.removeCallbacks(rDelete);
            });
            mSnackbar.setActionTextColor(Color.YELLOW);
            mSnackbar.show();

            mHandler.postDelayed(rDelete, 5000);
        } else if (!delete) {
            mDownloadManager.forgetFinishedDownloads();
            applyChanges();
        }
    }

    private void deleteFinishedDownloads() {
        if (mSnackbar != null)
            mSnackbar.dismiss();

        Iterator<Mission> i = mHidden.iterator();
        while (i.hasNext()) {
            Mission mission = i.next();
            if (mission != null) {
                mDownloadManager.deleteMission(mission);
                mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mission.storage.getUri())
                        .setPackage(mContext.getPackageName()));
            }
            i.remove();
        }
    }

    private boolean handlePopupItem(@NonNull ViewHolderItem h, @NonNull MenuItem option) {
        if (h.item == null)
            return true;

        int id = option.getItemId();
        DownloadMission mission = h.item.mission instanceof DownloadMission ? (DownloadMission) h.item.mission : null;

        if (mission != null) {
            switch (id) {
                case R.id.start:
                    h.status.setText(UNDEFINED_PROGRESS);
                    mDownloadManager.resumeMission(mission);
                    return true;
                case R.id.pause:
                    mDownloadManager.pauseMission(mission);
                    return true;
                case R.id.error_message_view:
                    showError(mission);
                    return true;
                case R.id.retry:
                    if (mission.isPsRunning()) {
                        mission.psContinue(true);
                    } else {
                        mDownloadManager.tryRecover(mission);
                        if (mission.storage.isInvalid())
                            mRecover.tryRecover(mission);
                        else
                            recoverMission(mission);
                    }
                    return true;
                case R.id.cancel:
                    mission.psContinue(false);
                    return false;
            }
        }

        switch (id) {
            case R.id.menu_item_share:
                shareFile(h.item.mission);
                return true;
            case R.id.delete:
                mDeleter.append(h.item.mission);
                applyChanges();
                checkMasterButtonsVisibility();
                return true;
            default:
                return false;
        }
    }

    public void applyChanges() {
        mIterator.start();
        DiffUtil.calculateDiff(mIterator, true).dispatchUpdatesTo(this);
        mIterator.end();

        if (mClear != null) {
            mClear.setVisible(mIterator.hasFinishedMissions());
        }
    }

    public void forceUpdate() {
        mIterator.start();
        mIterator.end();

        for (ViewHolderItem item : mPendingDownloadsItems) {
            item.resetSpeedMeasure();
        }

        notifyDataSetChanged();
    }

    public void setClearButton(MenuItem clearButton) {
        if (mClear == null)
            clearButton.setVisible(mIterator.hasFinishedMissions());

        mClear = clearButton;
    }

    public void setMasterButtons(MenuItem startButton, MenuItem pauseButton) {
        boolean init = mStartButton == null || mPauseButton == null;

        mStartButton = startButton;
        mPauseButton = pauseButton;

        if (init) {
            checkMasterButtonsVisibility();
        }
    }

    public void checkMasterButtonsVisibility() {
        boolean[] state = mIterator.hasValidPendingMissions();
        setButtonVisible(mPauseButton, state[0]);
        setButtonVisible(mStartButton, state[1]);
    }

    private static void setButtonVisible(MenuItem button, boolean visible) {
        if (button != null && button.isVisible() != visible)
            button.setVisible(visible);
    }

    public void refreshMissionItems() {
        for (ViewHolderItem h : mPendingDownloadsItems) {
            if (((DownloadMission) h.item.mission).running)
                continue;
            updateProgress(h);
            h.resetSpeedMeasure();
        }
    }

    public void onDestroy() {
        mDeleter.dispose();
        compositeDisposable.clear();
        compositeDisposable.dispose();
    }

    public void onResume() {
        mDeleter.resume();
        mHandler.post(rUpdater);
    }

    public void onPaused() {
        mDeleter.pause();
        mHandler.removeCallbacks(rUpdater);
    }

    public void recoverMission(DownloadMission mission) {
        ViewHolderItem h = getViewHolder(mission);
        if (h == null)
            return;

        mission.errObject = null;
        mission.resetState(true, false, ERROR_NOTHING);

        h.status.setText(UNDEFINED_PROGRESS);
        h.size.setText(Utility.formatBytes(mission.getLength()));
        h.progress.setMarquee(true);

        mDownloadManager.resumeMission(mission);
    }

    private void updater() {
        for (ViewHolderItem h : mPendingDownloadsItems) {
            // check if the mission is running first
            if (!((DownloadMission) h.item.mission).running)
                continue;

            updateProgress(h);
        }

        mHandler.postDelayed(rUpdater, 1000);
    }

    private boolean isNotFinite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value);
    }

    public void setRecover(@NonNull RecoverHelper callback) {
        mRecover = callback;
    }

    class ViewHolderItem extends ViewHolder {
        DownloadManager.MissionItem item;

        TextView status;
        ImageView icon;
        ImageView mediaIcon;
        TextView name;
        TextView size;
        ProgressBar progressIndicator;
        ProgressDrawable progress;

        PopupMenu popupMenu;
        MenuItem retry;
        MenuItem cancel;
        MenuItem start;
        MenuItem pause;
        MenuItem open;
        MenuItem showError;
        MenuItem delete;

        long lastTimestamp = -1;
        double lastDone;
        int lastSpeedIdx;
        float[] lastSpeed = new float[3];
        String estimatedTimeArrival = UNDEFINED_ETA;

        @SuppressLint("CheckResult")
        ViewHolderItem(View view) {
            super(view);

            progressIndicator = itemView.findViewById(R.id.progress_indicator);
            progress = new ProgressDrawable();
            ViewCompat.setBackground(progressIndicator, progress);

            status = itemView.findViewById(R.id.item_status);
            name = itemView.findViewById(R.id.item_name);
            icon = itemView.findViewById(R.id.item_icon);
            mediaIcon = itemView.findViewById(R.id.media_icon);
            size = itemView.findViewById(R.id.item_size);

            name.setSelected(true);

            ImageView button = itemView.findViewById(R.id.item_more);
            popupMenu = buildPopup(button);
            button.setOnClickListener(v -> showPopupMenu());

            Menu menu = popupMenu.getMenu();
            retry = menu.findItem(R.id.retry);
            cancel = menu.findItem(R.id.cancel);
            start = menu.findItem(R.id.start);
            pause = menu.findItem(R.id.pause);
            open = menu.findItem(R.id.menu_item_share);
            showError = menu.findItem(R.id.error_message_view);
            delete = menu.findItem(R.id.delete);

            itemView.setHapticFeedbackEnabled(true);

            itemView.setOnClickListener(v -> {
                if (item.mission instanceof FinishedMission) {
                    AdMobInterstitialAdSong.getInstance().init((Activity) mContext);
                    Utility.FileType type = Utility.getFileType(item.mission.kind);
                    if (Utility.FileType.VIDEO.equals(type)) {
                        AdMobInterstitialAdSong.getInstance().showInterstitialAd((Activity) mContext, new AdMobInterstitialAdSong.AdClosedListener() {
                                    @Override
                                    public void onAdClosed() {
                                        VideoPlayerActivity.playerList = new ArrayList<>();
                                        VideoPlayerActivity.position = 0;
                                        Video video = new Video("",
                                                FileUtil.getFileName(mContext, item.mission.storage.getUri()),
                                                0L,
                                                mContext.getString(R.string.app_name),
                                                "",
                                                "",
                                                item.mission.storage.getUri());
                                        VideoPlayerActivity.playerList.add(video);
                                        Intent playerIntent = new Intent(mContext, VideoPlayerActivity.class);
                                        playerIntent.putExtra("class", "NowPlaying");
                                        startActivity(mContext, playerIntent, null);
                                    }
                                });

                    } else if (Utility.FileType.MUSIC.equals(type)) {
                        MusicPlayerActivity.playerList = new ArrayList<>();
                        MusicPlayerActivity.position = 0;
                        Song song = new Song(0L,
                                FileUtil.getFileName(mContext, item.mission.storage.getUri()),
                                0,
                                0,
                                0L,
                                item.mission.storage.getUri().toString(),
                                0L,
                                0L,
                                "",
                                0L,
                                mContext.getString(R.string.app_name));

                        MusicPlayerActivity.playerList.add(song);
                        Intent playerIntent = new Intent(mContext, MusicPlayerActivity.class);
                        playerIntent.putExtra("class", "NowPlaying");
                        startActivity(mContext, playerIntent, null);
                    } else {
                        viewWithFileProvider(item.mission);
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                showPopupMenu();
                return true;
            });
        }

        private void showPopupMenu() {
            retry.setVisible(false);
            cancel.setVisible(false);
            start.setVisible(false);
            pause.setVisible(false);
            open.setVisible(false);
            showError.setVisible(false);
            delete.setVisible(false);

            DownloadMission mission = item.mission instanceof DownloadMission ? (DownloadMission) item.mission : null;

            if (mission != null) {
                if (mission.hasInvalidStorage()) {
                    retry.setVisible(true);
                    delete.setVisible(true);
                    showError.setVisible(true);
                } else if (mission.isPsRunning()) {
                    switch (mission.errCode) {
                        case ERROR_INSUFFICIENT_STORAGE:
                        case ERROR_POSTPROCESSING_HOLD:
                            retry.setVisible(true);
                            cancel.setVisible(true);
                            showError.setVisible(true);
                            break;
                    }
                } else {
                    if (mission.running) {
                        pause.setVisible(true);
                    } else {
                        if (mission.errCode != ERROR_NOTHING) {
                            showError.setVisible(true);
                        }

                        delete.setVisible(true);

                        boolean flag = !mission.isPsFailed() && mission.urls.length > 0;
                        start.setVisible(flag);
                    }
                }
            } else {
                open.setVisible(true);
                delete.setVisible(true);
            }

            popupMenu.show();
        }

        private PopupMenu buildPopup(final View button) {
            PopupMenu popup = new PopupMenu(mContext, button);
            popup.inflate(R.menu.menu_mission);
            popup.setOnMenuItemClickListener(option -> handlePopupItem(this, option));

            return popup;
        }

        private void resetSpeedMeasure() {
            estimatedTimeArrival = UNDEFINED_ETA;
            lastTimestamp = -1;
            lastSpeedIdx = -1;
        }
    }

    static class ViewHolderHeader extends ViewHolder {
        TextView header;

        ViewHolderHeader(View view) {
            super(view);
            header = itemView.findViewById(R.id.item_name);
        }
    }

    public interface RecoverHelper {
        void tryRecover(DownloadMission mission);
    }
}

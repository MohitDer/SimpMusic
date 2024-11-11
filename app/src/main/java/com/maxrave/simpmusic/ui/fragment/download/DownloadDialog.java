package com.maxrave.simpmusic.ui.fragment.download;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.downloader.helper.MissionRecoveryInfo;
import com.maxrave.simpmusic.downloader.io.StoredDirectoryHelper;
import com.maxrave.simpmusic.downloader.io.StoredFileHelper;
import com.maxrave.simpmusic.downloader.processor.PostProcessing;
import com.maxrave.simpmusic.downloader.service.DownloadManager;
import com.maxrave.simpmusic.downloader.service.DownloadManagerService;
import com.maxrave.simpmusic.downloader.service.MissionState;
import com.maxrave.simpmusic.downloader.util.FilenameUtils;
import com.maxrave.simpmusic.downloader.util.ImageUtils;
import com.maxrave.simpmusic.downloader.util.ListHelper;
import com.maxrave.simpmusic.downloader.util.Localization;
import com.maxrave.simpmusic.downloader.util.PermissionHelper;
import com.maxrave.simpmusic.downloader.util.SecondaryStreamHelper;
import com.maxrave.simpmusic.downloader.util.StreamSizeWrapper;

import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import icepick.Icepick;
import icepick.State;
import io.reactivex.disposables.CompositeDisposable;

public class DownloadDialog extends BottomSheetDialogFragment implements AudioStreamAdapter.Listener, VideoStreamAdapter.Listener {
    private static final String TAG = "DialogFragment";

    @State
    protected StreamInfo currentInfo;
    @State
    protected StreamSizeWrapper<AudioStream> wrappedAudioStreams = StreamSizeWrapper.empty();
    @State
    protected StreamSizeWrapper<VideoStream> wrappedVideoStreams = StreamSizeWrapper.empty();
    @State
    protected int selectedVideoIndex = 0;
    @State
    protected int selectedAudioIndex = 0;

    private AudioStreamAdapter<AudioStream, Stream> audioStreamsAdapter;
    private VideoStreamAdapter<VideoStream, AudioStream> videoStreamsAdapter;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private EditText fileName;
    private View audioHeader;
    private RecyclerView recyclerViewAudio;
    private View videoHeader;
    private RecyclerView recyclerViewVideo;

    public static DownloadDialog newInstance(StreamInfo info) {
        DownloadDialog dialog = new DownloadDialog();
        dialog.setInfo(info);
        return dialog;
    }

    public static DownloadDialog newInstance(Context context, StreamInfo info) {
        final ArrayList<VideoStream> streamsList = new ArrayList<>(ListHelper.getSortedStreamVideosList(context, info.getVideoStreams(), info.getVideoOnlyStreams(), false, false));
//        final int selectedStreamIndex = ListHelper.getDefaultResolutionIndex(context, streamsList);

        final DownloadDialog instance = newInstance(info);
        instance.setVideoStreams(streamsList);
//        instance.setSelectedVideoStream(selectedStreamIndex);
        instance.setAudioStreams(com.annimon.stream.Stream.of(info.getAudioStreams()).limit(2).toList());
        instance.setSelectedAudioStream(0);

        return instance;
    }

    private void setInfo(StreamInfo info) {
        this.currentInfo = info;
    }

    public void setAudioStreams(List<AudioStream> audioStreams) {
        setAudioStreams(new StreamSizeWrapper<>(audioStreams, context));
    }

    public void setAudioStreams(StreamSizeWrapper<AudioStream> wrappedAudioStreams) {
        this.wrappedAudioStreams = wrappedAudioStreams;
    }

    public void setVideoStreams(List<VideoStream> videoStreams) {
        setVideoStreams(new StreamSizeWrapper<>(videoStreams, context));
    }

    public void setVideoStreams(StreamSizeWrapper<VideoStream> wrappedVideoStreams) {
        this.wrappedVideoStreams = wrappedVideoStreams;
    }

    public void setSelectedVideoStream(int selectedVideoIndex) {
        this.selectedVideoIndex = selectedVideoIndex;
    }

    public void setSelectedAudioStream(int selectedAudioIndex) {
        this.selectedAudioIndex = selectedAudioIndex;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PermissionHelper.checkStoragePermissions(getActivity(), PermissionHelper.DOWNLOAD_DIALOG_REQUEST_CODE)) {
            if (getDialog() != null) {
                getDialog().dismiss();
            }
            return;
        }

        context = getContext();

        setStyle(STYLE_NO_TITLE, R.style.DarkBottomSheetDialog);
        Icepick.restoreInstanceState(this, savedInstanceState);

        SparseArray<SecondaryStreamHelper<AudioStream>> secondaryStreams = new SparseArray<>(1);
        List<VideoStream> videoStreams = wrappedVideoStreams.getStreamsList();

        for (int i = 0; i < videoStreams.size(); i++) {
            if (!videoStreams.get(i).isVideoOnly())
                continue;

            AudioStream audioStream = SecondaryStreamHelper.getAudioStreamFor(wrappedAudioStreams.getStreamsList(), videoStreams.get(i));
            if (audioStream != null) {
                secondaryStreams.append(i, new SecondaryStreamHelper<>(wrappedAudioStreams, audioStream));
            }
        }

        this.audioStreamsAdapter = new AudioStreamAdapter<>(context, wrappedAudioStreams, selectedAudioIndex, this);
        this.videoStreamsAdapter = new VideoStreamAdapter<>(context, wrappedVideoStreams, secondaryStreams, -1, this);

        Intent intent = new Intent(context, DownloadManagerService.class)
                .setPackage(context.getPackageName());
        context.startService(intent);

        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName cname, IBinder service) {
                DownloadManagerService.DownloadManagerBinder mgr = (DownloadManagerService.DownloadManagerBinder) service;

                mainStorageAudio = mgr.getMainStorageAudio();
                mainStorageVideo = mgr.getMainStorageVideo();
                downloadManager = mgr.getDownloadManager();

                btnDownload.setEnabled(true);

                context.unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // nothing to do
            }
        }, Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.download_dialog, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            if (dialog != null) {
                FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    final BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setPeekHeight(0, true);
                    behavior.setDraggable(false);
                    behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                        @Override
                        public void onStateChanged(@NonNull View bottomSheet, int newState) {
                            if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            }
                        }

                        @Override
                        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                        }
                    });
                }
            }
        });

        audioHeader = view.findViewById(R.id.audioHeader);
        videoHeader = view.findViewById(R.id.videoHeader);

        ImageView thumbnail = view.findViewById(R.id.thumbnail);
        TextView duration = view.findViewById(R.id.duration);
        fileName = view.findViewById(R.id.file_name);
        fileName.setText(FilenameUtils.createFilename(context, currentInfo.getName()));
        ImageUtils.loadThumbnail(getActivity(), thumbnail, currentInfo.getThumbnails());
        if (currentInfo.getDuration() > 0) {
            duration.setText(Localization.getDurationString(currentInfo.getDuration()));
            duration.setBackgroundResource(R.drawable.duration_background);
            duration.setVisibility(View.VISIBLE);
        } else {
            duration.setVisibility(View.GONE);
        }

        recyclerViewAudio = view.findViewById(R.id.recyclerview_audio);
        recyclerViewAudio.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        recyclerViewAudio.setAdapter(audioStreamsAdapter);

        recyclerViewVideo = view.findViewById(R.id.recyclerview_video);
        recyclerViewVideo.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        recyclerViewVideo.setAdapter(videoStreamsAdapter);

        selectedAudioIndex = ListHelper.getDefaultAudioFormat(context, currentInfo.getAudioStreams());

        recyclerViewAudio = view.findViewById(R.id.recyclerview_audio);
        recyclerViewVideo = view.findViewById(R.id.recyclerview_video);

        btnDownload = view.findViewById(R.id.btn_download);
        btnDownload.setEnabled(false);

        setupDownloadOptions();
        fetchStreamsSize();

        // download
        btnDownload.setOnClickListener(v -> {
            prepareSelectedDownload();
        });

        view.findViewById(R.id.container).setOnClickListener(v -> hideKeyboard());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchStreamsSize() {
        disposables.add(StreamSizeWrapper.fetchSizeForWrapper(wrappedVideoStreams)
                .subscribe(result -> videoStreamsAdapter.notifyDataSetChanged()));
        disposables.add(StreamSizeWrapper.fetchSizeForWrapper(wrappedAudioStreams)
                .subscribe(result -> audioStreamsAdapter.notifyDataSetChanged()));
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(fileName.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }

    protected void setupDownloadOptions() {
        final boolean isVideoStreamsAvailable = !videoStreamsAdapter.getAll().isEmpty();
        final boolean isAudioStreamsAvailable = !audioStreamsAdapter.getAll().isEmpty();

        audioHeader.setVisibility(isAudioStreamsAvailable ? View.VISIBLE : View.GONE);
        recyclerViewAudio.setVisibility(isAudioStreamsAvailable ? View.VISIBLE : View.GONE);
        videoHeader.setVisibility(isVideoStreamsAvailable ? View.VISIBLE : View.GONE);
        recyclerViewVideo.setVisibility(isVideoStreamsAvailable ? View.VISIBLE : View.GONE);

        if (!isVideoStreamsAvailable && !isAudioStreamsAvailable) {
            Toast.makeText(context, R.string.no_streams_available_download, Toast.LENGTH_SHORT).show();
            if (getDialog() != null) {
                getDialog().dismiss();
            }
        }
    }

    private StoredDirectoryHelper mainStorageAudio = null;
    private StoredDirectoryHelper mainStorageVideo = null;
    private DownloadManager downloadManager = null;
    private MaterialButton btnDownload = null;
    private Context context;

    private String getFileName() {
        String str = fileName.getText().toString().trim();
        return FilenameUtils.createFilename(context, str.isEmpty() ? currentInfo.getName() : str);
    }

    private void showFailedDialog(@StringRes int msg) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.something_went_wrong)
                .setMessage(msg)
                .setNegativeButton(getString(R.string.ok), null)
                .create()
                .show();
    }

    private void showErrorActivity(Exception e) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.something_went_wrong)
                .setMessage(e.getMessage())
                .setNegativeButton(getString(R.string.ok), null)
                .create()
                .show();
    }

    private void prepareSelectedDownload() {
        StoredDirectoryHelper mainStorage;
        MediaFormat format;
        String mime;

        // first, build the filename and get the output folder (if possible)
        // later, run a very very very large file checking logic
        String filename = getFileName().concat(".");
        if (audioStreamsAdapter.getSelected() != null) {
            mainStorage = mainStorageAudio;
            format = audioStreamsAdapter.getItem(selectedAudioIndex).getFormat();
            if (selectedAudioIndex == 1){
                mime = "audio/mp3";
                filename += "mp3";
            }else {
                if (format == MediaFormat.WEBMA_OPUS) {
                    mime = "audio/ogg";
                    filename += "opus";
                } else {
                    mime = format.mimeType;
                    filename += format.suffix;
                }
            }

        } else {
            mainStorage = mainStorageVideo;
            format = videoStreamsAdapter.getItem(selectedVideoIndex).getFormat();
            mime = format.mimeType;
            filename += format.suffix;
        }

        if (filename.contains(" ") || filename.contains("'") || filename.contains("\"")) {
            filename = filename.replace(" ", "")   // Remove spaces
                    .replace("'", "")   // Remove single quotes
                    .replace("\"", ""); // Remove double quotes
        }
        // check for existing file with the same name
        checkSelectedDownload(mainStorage, mainStorage.findFile(filename), filename, mime);
    }

    private void checkSelectedDownload(StoredDirectoryHelper mainStorage, Uri targetFile, String filename, String mime) {
        StoredFileHelper storage;

        try {
            if (mainStorage == null) {
                // using SAF on older android version
                storage = new StoredFileHelper(context, null, targetFile, "");
            } else if (targetFile == null) {
                // the file does not exist, but it is probably used in a pending download
                storage = new StoredFileHelper(mainStorage.getUri(), filename, mime, mainStorage.getTag());
            } else {
                // the target filename is already use, attempt to use it
                storage = new StoredFileHelper(context, mainStorage.getUri(), targetFile, mainStorage.getTag());
            }
        } catch (Exception e) {
            showErrorActivity(e);
            return;
        }

        // check if is our file
        MissionState state = downloadManager.checkForExistingMission(storage);
        @StringRes int msgBtn;
        @StringRes int msgBody;

        switch (state) {
            case Finished:
                msgBtn = R.string.overwrite;
                msgBody = R.string.overwrite_finished_warning;
                break;
            case Pending:
                msgBtn = R.string.overwrite;
                msgBody = R.string.download_already_pending;
                break;
            case PendingRunning:
                msgBtn = R.string.generate_unique_name;
                msgBody = R.string.download_already_running;
                break;
            case None:
                if (mainStorage == null) {
                    // This part is called if:
                    // * using SAF on older android version
                    // * save path not defined
                    // * if the file exists overwrite it, is not necessary ask
                    if (!storage.existsAsFile() && !storage.create()) {
                        showFailedDialog(R.string.error_file_creation);
                        return;
                    }
                    continueSelectedDownload(storage);
                    return;
                } else if (targetFile == null) {
                    // This part is called if:
                    // * the filename is not used in a pending/finished download
                    // * the file does not exists, create

                    if (!mainStorage.mkdirs()) {
                        showFailedDialog(R.string.error_path_creation);
                        return;
                    }

                    storage = mainStorage.createFile(filename, mime);
                    if (storage == null || !storage.canWrite()) {
                        showFailedDialog(R.string.error_file_creation);
                        return;
                    }

                    continueSelectedDownload(storage);
                    return;
                }
                msgBtn = R.string.overwrite;
                msgBody = R.string.overwrite_unrelated_warning;
                break;
            default:
                return;
        }


        AlertDialog.Builder askDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.download)
                .setMessage(msgBody)
                .setNegativeButton(android.R.string.cancel, null);
        final StoredFileHelper finalStorage = storage;


        if (mainStorage == null) {
            // This part is called if:
            // * using SAF on older android version
            // * save path not defined
            switch (state) {
                case Pending:
                case Finished:
                    askDialog.setPositiveButton(msgBtn, (dialog, which) -> {
                        dialog.dismiss();
                        downloadManager.forgetMission(finalStorage);
                        continueSelectedDownload(finalStorage);
                    });
                    break;
            }

            askDialog.create().show();
            return;
        }

        String finalFilename = filename;
        askDialog.setPositiveButton(msgBtn, (dialog, which) -> {
            dialog.dismiss();

            StoredFileHelper storageNew;
            switch (state) {
                case Finished:
                case Pending:
                    downloadManager.forgetMission(finalStorage);
                case None:
                    if (targetFile == null) {
                        storageNew = mainStorage.createFile(finalFilename, mime);
                    } else {
                        try {
                            // try take (or steal) the file
                            storageNew = new StoredFileHelper(context, mainStorage.getUri(), targetFile, mainStorage.getTag());
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to take (or steal) the file in " + targetFile.toString());
                            storageNew = null;
                        }
                    }

                    if (storageNew != null && storageNew.canWrite())
                        continueSelectedDownload(storageNew);
                    else
                        showFailedDialog(R.string.error_file_creation);
                    break;
                case PendingRunning:
                    storageNew = mainStorage.createUniqueFile(finalFilename, mime);
                    if (storageNew == null)
                        showFailedDialog(R.string.error_file_creation);
                    else
                        continueSelectedDownload(storageNew);
                    break;
            }
        });

        askDialog.create().show();
    }

    private void continueSelectedDownload(@NonNull StoredFileHelper storage) {
        if (!storage.canWrite()) {
            showFailedDialog(R.string.permission_denied);
            return;
        }

        // check if the selected file has to be overwritten, by simply checking its length
        try {
            if (storage.length() > 0)
                storage.truncate();
        } catch (IOException e) {
            Log.e(TAG, "failed to truncate the file: " + storage.getUri().toString(), e);
            showFailedDialog(R.string.overwrite_failed);
            return;
        }

        Stream selectedStream;
        Stream secondaryStream = null;
        char kind;
        String[] urls;
        MissionRecoveryInfo[] recoveryInfo;
        String psName = null;
        String[] psArgs = null;
        long nearLength = 0;

        if (audioStreamsAdapter.getSelected() != null) {
            kind = 'a';
            selectedStream = audioStreamsAdapter.getItem(selectedAudioIndex);

            if (selectedStream.getFormat() == MediaFormat.M4A) {
                psName = PostProcessing.ALGORITHM_M4A_NO_DASH;
            } else if (selectedStream.getFormat() == MediaFormat.WEBMA_OPUS) {
                psName = PostProcessing.ALGORITHM_OGG_FROM_WEBM_DEMUXER;
            }
        } else {
            kind = 'v';
            selectedStream = videoStreamsAdapter.getItem(selectedVideoIndex);

            SecondaryStreamHelper<AudioStream> secondary = videoStreamsAdapter
                    .getAllSecondary()
                    .get(wrappedVideoStreams.getStreamsList().indexOf(selectedStream));

            if (secondary != null) {
                secondaryStream = secondary.getStream();

                if (selectedStream.getFormat() == MediaFormat.MPEG_4) {
                    psName = PostProcessing.ALGORITHM_MP4_FROM_DASH_MUXER;
                } else {
                    psName = PostProcessing.ALGORITHM_WEBM_MUXER;
                }

                psArgs = null;
                long videoSize = wrappedVideoStreams.getSizeInBytes((VideoStream) selectedStream);

                // set nearLength, only, if both sizes are fetched or known. This probably
                // does not work on slow networks but is later updated in the downloader
                if (secondary.getSizeInBytes() > 0 && videoSize > 0) {
                    nearLength = secondary.getSizeInBytes() + videoSize;
                }
            }
        }

        if (secondaryStream == null) {
            urls = new String[]{selectedStream.getUrl()};
            recoveryInfo = new MissionRecoveryInfo[]{new MissionRecoveryInfo(selectedStream)};
        } else {
            urls = new String[]{selectedStream.getUrl(), secondaryStream.getUrl()};
            recoveryInfo = new MissionRecoveryInfo[]{new MissionRecoveryInfo(selectedStream), new MissionRecoveryInfo(secondaryStream)};
        }

        Toast.makeText(context, getString(R.string.start_downloads) + " " + currentInfo.getName(), Toast.LENGTH_SHORT).show();
        DownloadManagerService.startMission(context, urls, storage, kind, 10, currentInfo.getUrl(), psName, psArgs, nearLength, recoveryInfo);
        dismiss();
    }

    @Override
    public void onAudioSelected(int position) {
        videoStreamsAdapter.clearSelection();
        selectedAudioIndex = position;
    }

    @Override
    public void onVideoSelected(int position) {
        audioStreamsAdapter.clearSelection();
        selectedVideoIndex = position;
    }
}

package com.maxrave.simpmusic.ui.fragment.download.songs;

import static com.maxrave.simpmusic.ui.fragment.download.songs.FileUtil.AUDIO_FILE_FILTER;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.ads.admob.AdMobInterstitialAdSong;
import com.maxrave.simpmusic.databinding.FragmentLocalSongsBinding;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.maxrave.simpmusic.ui.fragment.download.player.MusicPlayerActivity;

public class SongsFragment extends Fragment implements LocalSongAdapter.Listener, LoaderManager.LoaderCallbacks<List<Song>> {

    private FragmentLocalSongsBinding binding;
    private LocalSongAdapter songAdapter;

    @NonNull
    public static SongsFragment getInstance() {
        return new SongsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_local_songs, container, false);
        binding = FragmentLocalSongsBinding.bind(view);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Init recycler view
        AdMobInterstitialAdSong.getInstance().init(requireActivity());
        initRecyclerView();

        scanAudioFiles();

        binding.swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.youtube_primary_color));
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            songAdapter.clear();
            scanAudioFiles();
            binding.swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void scanAudioFiles() {
        Context context = getActivity().getApplicationContext();
        binding.loadingProgressBar.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isDetached() && context != null && binding != null) {
                new ArrayListPathsAsyncTask(paths -> {
                    MediaScannerConnection.scanFile(context.getApplicationContext(), paths, null, null);
                    if (isAdded()) {
                        LoaderManager.getInstance(this).initLoader(0, null, this);
                    }
                }).execute(new ArrayListPathsAsyncTask.LoadingInfo(FileUtil.getDefaultStartDirectory(), AUDIO_FILE_FILTER));
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        binding.loadingProgressBar.setVisibility(View.GONE);
                    }
                });
            }
        }, 1000);
    }

    private void initRecyclerView() {
        songAdapter = new LocalSongAdapter(requireContext(), this);
        binding.itemsList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.itemsList.setAdapter(songAdapter);
    }

    private void showEmptyViews() {
        binding.emptyStateView.getRoot().setVisibility(songAdapter.isEmpty() ? View.VISIBLE : View.GONE);
        binding.itemsList.setVisibility(songAdapter.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
        if (isAdded()) {
            return new AsyncSongLoader(requireContext());
        }
        return null; // Or handle the case where the fragment is not attached.
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                songAdapter.setSongs(data.stream()
                        .filter(song -> song.duration > 20000L)
                        .collect(Collectors.toList())
                );
                showEmptyViews();
            });
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                songAdapter.setSongs(new ArrayList<>());
                showEmptyViews();
            });
        }
    }

    @Override
    public void onItemSelected(int position) {
        AdMobInterstitialAdSong.getInstance().showInterstitialAd(getActivity(), new AdMobInterstitialAdSong.AdClosedListener() {
            @Override
            public void onAdClosed() {
                MusicPlayerActivity.playerList = (ArrayList<Song>) songAdapter.getSongs();
                MusicPlayerActivity.position = position;
                Intent intent = new Intent(requireContext(), MusicPlayerActivity.class);
                intent.putExtra("class", "NowPlaying");
                ContextCompat.startActivity(requireContext(), intent, null);
            }
        });

    }

    public static class ArrayListPathsAsyncTask extends AsyncTask<ArrayListPathsAsyncTask.LoadingInfo, String, String[]> {

        private final WeakReference<OnPathsListedCallback> onPathsListedCallbackWeakReference;

        public ArrayListPathsAsyncTask(OnPathsListedCallback callback) {
            super();
            onPathsListedCallbackWeakReference = new WeakReference<>(callback);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            checkCallbackReference();
        }

        @Override
        protected String[] doInBackground(LoadingInfo... params) {
            try {
                if (isCancelled() || checkCallbackReference() == null)
                    return null;

                LoadingInfo info = params[0];

                final String[] paths;

                if (info.file.isDirectory()) {
                    List<File> files = FileUtil.listFilesDeep(info.file, info.fileFilter);

                    if (isCancelled() || checkCallbackReference() == null)
                        return null;

                    paths = new String[files.size()];
                    for (int i = 0; i < files.size(); i++) {
                        File f = files.get(i);
                        paths[i] = FileUtil.safeGetCanonicalPath(f);

                        if (isCancelled() || checkCallbackReference() == null)
                            return null;
                    }
                } else {
                    paths = new String[1];
                    paths[0] = FileUtil.safeGetCanonicalPath(info.file);
                }

                return paths;
            } catch (Exception e) {
                cancel(false);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] paths) {
            super.onPostExecute(paths);
            OnPathsListedCallback callback = checkCallbackReference();
            if (callback != null && paths != null && !getInstance().isDetached()) {
                callback.onPathsListed(paths);
            }
        }

        private OnPathsListedCallback checkCallbackReference() {
            OnPathsListedCallback callback = onPathsListedCallbackWeakReference.get();
            if (callback == null) {
                cancel(false);
            }
            return callback;
        }

        public static class LoadingInfo {
            public final File file;
            public final FileFilter fileFilter;

            public LoadingInfo(File file, FileFilter fileFilter) {
                this.file = file;
                this.fileFilter = fileFilter;
            }
        }

        public interface OnPathsListedCallback {
            void onPathsListed(@NonNull String[] paths);
        }
    }

    private static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

        public AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Song> loadInBackground() {
            return SongLoader.getAllSongs(getContext());
        }
    }
}

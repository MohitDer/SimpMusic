package com.maxrave.simpmusic.ui.fragment.download;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.downloader.helper.DownloadMission;
import com.maxrave.simpmusic.downloader.io.StoredFileHelper;
import com.maxrave.simpmusic.downloader.service.DownloadManagerService;
import com.maxrave.simpmusic.downloader.service.DownloadManagerService.DownloadManagerBinder;
import com.nononsenseapps.filepicker.Utils;

import java.io.IOException;

public class MissionsFragment extends Fragment {

    public static MissionsFragment getInstance() {
        return new MissionsFragment();
    }

    private static final int REQUEST_DOWNLOAD_SAVE_AS = 0x1230;

    private MenuItem mClear = null;
    private MenuItem mStart = null;
    private MenuItem mPause = null;

    private RecyclerView mList;
    private MissionAdapter mAdapter;
    private LinearLayoutManager mLinearManager;
    private Context mContext;

    private DownloadManagerService.DownloadManagerBinder mBinder;
    private boolean mForceUpdate;

    private DownloadMission unsafeMissionTarget = null;

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mBinder = (DownloadManagerBinder) binder;
            mBinder.clearDownloadNotifications();

            mAdapter = new MissionAdapter(mContext, mBinder.getDownloadManager(), getView());
            mAdapter.setRecover(MissionsFragment.this::recoverMission);

            setAdapterButtons();

            mBinder.addMissionEventListener(mAdapter);
            mBinder.enableNotifications(false);

            updateList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // What to do?
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_missions, container, false);

        // Bind the service
        mContext.bindService(new Intent(mContext, DownloadManagerService.class)
                .setPackage(mContext.getPackageName()), mConnection, Context.BIND_AUTO_CREATE);

        // Views
        mList = v.findViewById(R.id.mission_recycler);

        // Init layouts managers
        mLinearManager = new LinearLayoutManager(getActivity());

        setHasOptionsMenu(true);

        return v;
    }

    /**
     * Added in API level 23.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // Bug: in api< 23 this is never called
        // so mActivity=null
        // so app crashes with null-pointer exception
        mContext = context;
    }

    /**
     * deprecated in API level 23,
     * but must remain to allow compatibility with api<23
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);

        mContext = activity;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBinder == null || mAdapter == null)
            return;

        mBinder.removeMissionEventListener(mAdapter);
        mBinder.enableNotifications(true);
        mContext.unbindService(mConnection);
        mAdapter.onDestroy();

        mBinder = null;
        mAdapter = null;
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        mClear = menu.findItem(R.id.clear_list);
        mStart = menu.findItem(R.id.start_downloads);
        mPause = menu.findItem(R.id.pause_downloads);

        if (mAdapter != null)
            setAdapterButtons();

        super.onPrepareOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_list:
                MaterialAlertDialogBuilder prompt = getMaterialAlertDialogBuilder();
                prompt.setNegativeButton(R.string.cancel, null);
                prompt.create().show();
                return true;
            case R.id.start_downloads:
                mBinder.getDownloadManager().startAllMissions();
                return true;
            case R.id.pause_downloads:
                mBinder.getDownloadManager().pauseAllMissions(false);
                mAdapter.refreshMissionItems();// update items view
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @NonNull
    private MaterialAlertDialogBuilder getMaterialAlertDialogBuilder() {
        MaterialAlertDialogBuilder prompt = new MaterialAlertDialogBuilder(mContext);
        prompt.setTitle(R.string.delete_all_downloads);
        prompt.setMessage(R.string.confirm_prompt);
        // Intentionally misusing button's purpose in order to achieve good order
        //prompt.setNegativeButton(R.string.clear_download_history, (dialog, which) -> mAdapter.clearFinishedDownloads(false));
        prompt.setPositiveButton(R.string.delete_downloaded_files, (dialog, which) -> mAdapter.clearFinishedDownloads(true));
        return prompt;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateList() {
        mList.setLayoutManager(mLinearManager);
        // destroy all created views in the recycler
        mList.setAdapter(null);
        mAdapter.notifyDataSetChanged();

        // re-attach the adapter in grid/lineal mode
        mList.setAdapter(mAdapter);
    }

    private void setAdapterButtons() {
        if (mClear == null || mStart == null || mPause == null)
            return;

        mAdapter.setClearButton(mClear);
        mAdapter.setMasterButtons(mStart, mPause);
    }

    private void recoverMission(@NonNull DownloadMission mission) {
        unsafeMissionTarget = mission;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdapter != null) {
            mAdapter.onResume();

            if (mForceUpdate) {
                mForceUpdate = false;
                mAdapter.forceUpdate();
            }

            mBinder.addMissionEventListener(mAdapter);
            mAdapter.checkMasterButtonsVisibility();
        }
        if (mBinder != null)
            mBinder.enableNotifications(false);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mAdapter != null) {
            mForceUpdate = true;
            mBinder.removeMissionEventListener(mAdapter);
            mAdapter.onPaused();
        }

        if (mBinder != null)
            mBinder.enableNotifications(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_DOWNLOAD_SAVE_AS || resultCode != Activity.RESULT_OK)
            return;

        if (unsafeMissionTarget == null || data.getData() == null) {
            return;
        }

        try {
            Uri fileUri = data.getData();
            if (fileUri.getAuthority() != null) {
                fileUri = Uri.fromFile(Utils.getFileForUri(fileUri));
            }

            String tag = unsafeMissionTarget.storage.getTag();
            unsafeMissionTarget.storage = new StoredFileHelper(mContext, null, fileUri, tag);
            mAdapter.recoverMission(unsafeMissionTarget);
        } catch (IOException e) {
            Toast.makeText(mContext, R.string.something_went_wrong, Toast.LENGTH_LONG).show();
        }
    }
}

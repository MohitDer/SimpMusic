package com.maxrave.simpmusic.downloader.common;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.downloader.helper.FinishedMission;
import com.maxrave.simpmusic.downloader.helper.Mission;
import com.maxrave.simpmusic.downloader.service.DownloadManager;
import com.maxrave.simpmusic.downloader.service.DownloadManager.MissionIterator;
import com.maxrave.simpmusic.ui.fragment.download.MissionAdapter;

import java.util.ArrayList;

public class Deleter {
    private static final int TIMEOUT = 5000;// ms
    private static final int DELAY = 350;// ms
    private static final int DELAY_RESUME = 400;// ms

    private Snackbar snackbar;
    private ArrayList<Mission> items;
    private boolean running = true;

    private final Context mContext;
    private final MissionAdapter mAdapter;
    private final DownloadManager mDownloadManager;
    private final MissionIterator mIterator;
    private final Handler mHandler;
    private final View mView;

    private final Runnable rShow;
    private final Runnable rNext;
    private final Runnable rCommit;

    public Deleter(View v, Context c, MissionAdapter a, DownloadManager d, MissionIterator i, Handler h) {
        mView = v;
        mContext = c;
        mAdapter = a;
        mDownloadManager = d;
        mIterator = i;
        mHandler = h;

        // use variables to know the reference of the lambdas
        rShow = this::show;
        rNext = this::next;
        rCommit = this::commit;

        items = new ArrayList<>(2);
    }

    public void append(Mission item) {
        mIterator.hide(item);
        items.add(0, item);

        show();
    }

    private void forget() {
        mIterator.unHide(items.remove(0));
        mAdapter.applyChanges();

        show();
    }

    private void show() {
        if (items.isEmpty()) return;

        pause();
        running = true;

        mHandler.postDelayed(rNext, DELAY);
    }

    private void next() {
        if (items.isEmpty()) return;

        String msg = mContext.getString(R.string.video_deleted_from_downloads);
        snackbar = Snackbar.make(mView, msg, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.undo, s -> forget());
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();

        mHandler.postDelayed(rCommit, TIMEOUT);
    }

    private void commit() {
        if (items.isEmpty()) return;

        while (!items.isEmpty()) {
            Mission mission = items.remove(0);
            if (mission.deleted) continue;

            mIterator.unHide(mission);
            mDownloadManager.deleteMission(mission);

            if (mission instanceof FinishedMission) {
                mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mission.storage.getUri())
                        .setPackage(mContext.getPackageName()));
            }
            break;
        }

        if (items.isEmpty()) {
            pause();
            return;
        }

        show();
    }

    public void pause() {
        running = false;
        mHandler.removeCallbacks(rNext);
        mHandler.removeCallbacks(rShow);
        mHandler.removeCallbacks(rCommit);
        if (snackbar != null) snackbar.dismiss();
    }

    public void resume() {
        if (running) return;
        mHandler.postDelayed(rShow, DELAY_RESUME);
    }

    public void dispose() {
        if (items.isEmpty()) return;
        pause();
        for (Mission mission : items) mDownloadManager.deleteMission(mission);
        items = null;
    }
}

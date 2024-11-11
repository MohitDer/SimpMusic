package com.maxrave.simpmusic.ui.fragment.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.ui.fragment.download.songs.SongsFragment;

import com.maxrave.simpmusic.ui.fragment.download.videos.LocalVideosFragment;

public class LocalAdapter extends FragmentStatePagerAdapter {

    private final Context context;

    public LocalAdapter(FragmentManager fm, Context context) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.context = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {

        switch (position) {

            case 0:
                return MissionsFragment.getInstance();

            case 1:
                return SongsFragment.getInstance();

            case 2:
                return new LocalVideosFragment();

            default:
                return new Fragment();
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getString(R.string.download);

            case 1:
                return context.getString(R.string.songs);

            case 2:
                return context.getString(R.string.videos);

            default:
                return "";
        }
    }
}

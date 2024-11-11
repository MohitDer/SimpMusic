package com.maxrave.simpmusic.ui.fragment.download;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.media3.common.util.UnstableApi;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.ads.admob.AdMobConfig;
import com.maxrave.simpmusic.ads.admob.AdMobInterstitialAdSong;
import com.maxrave.simpmusic.ads.config.AdConfig;
import com.maxrave.simpmusic.databinding.ActivityDownloaderBinding;
import com.maxrave.simpmusic.downloader.service.DownloadManagerService;
import com.maxrave.simpmusic.downloader.util.PermissionHelper;
import com.maxrave.simpmusic.ui.MainActivity;
import com.maxrave.simpmusic.utils.SharedPrefsHelper;

public class DownloadActivity extends AppCompatActivity {

    public enum Extra {HOME_FEED}

    private ActivityDownloaderBinding binding;
    private LocalAdapter adapter;

    AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Service
        Intent i = new Intent();
        i.setPackage(getPackageName());
        i.setClass(this, DownloadManagerService.class);
        startService(i);

        super.onCreate(savedInstanceState);
        binding = ActivityDownloaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AdMobInterstitialAdSong.getInstance().init(this);

        Toolbar toolbar = findViewById(R.id.default_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.downloaded);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        PermissionHelper.checkStoragePermissions(this, PermissionHelper.DOWNLOAD_DIALOG_REQUEST_CODE);

        Intent bundle = getIntent();
        int selectedTab = 0; // Default tab

        if (bundle != null) {
            String type = bundle.getStringExtra("type");
            if ("audio".equals(type)) {
                selectedTab = 1; // Select the "Songs" tab (index 1)
            } else if ("video".equals(type)) {
                selectedTab = 2; // Select the "Videos" tab (index 2)
            }
        }


        adapter = new LocalAdapter(getSupportFragmentManager(), this);
        binding.viewPager.setOffscreenPageLimit(1);
        binding.viewPager.setAdapter(adapter);
        binding.tabLayout.setupWithViewPager(binding.viewPager);

        binding.viewPager.setCurrentItem(selectedTab);

        boolean extras = getIntent().getBooleanExtra(Extra.HOME_FEED.name(), false);
        if (extras) {
            binding.viewPager.setCurrentItem(2);
        }

        loadAds();
//        LanguagePreference.setAppLanguage(this,LanguagePreference.getSelectedLanguage(this));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_download, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("MissingSuperCall")
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction("com.maxrave.simpmusic.action.LIBRARY");
        startActivity(intent);
    }

    public void loadAds() {

        AdConfig adConfig = (AdConfig) SharedPrefsHelper.loadObjectPrefs(this, SharedPrefsHelper.Key.Native_Home.name(), AdConfig.class);
        if (adConfig != null && adConfig.Show) {
            AdMobConfig.showNativeAd(this, binding.templateView, adConfig.Ad_Id);
        }

        binding.adViewContainer.removeAllViews();
        adConfig = (AdConfig) SharedPrefsHelper.loadObjectPrefs(this, SharedPrefsHelper.Key.Banner_PlayerView.name(), AdConfig.class);
        if (adConfig != null && adConfig.Show) {
            adView = new AdView(this);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.setAdSize(AdSize.FULL_BANNER);
            String adId = adConfig.Ad_Id;
            adView.setAdUnitId(adId);
            adView.setAdListener(new AdListener() {

                @Override
                public void onAdLoaded() {
                    // Code to be executed when an ad finishes loading.
                    binding.adViewContainer.addView(adView);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    // Code to be executed when an ad request fails.
                    binding.adViewContainer.removeAllViews();
                }
            });
            adView.loadAd(adRequest);
        } else {
            binding.adViewContainer.removeAllViews();
        }

    }


}

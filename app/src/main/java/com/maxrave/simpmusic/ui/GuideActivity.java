

package com.maxrave.simpmusic.ui;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.media3.common.util.UnstableApi;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.adapter.GuideAdapter;
import com.maxrave.simpmusic.adapter.GuideModel;
import com.maxrave.simpmusic.ads.admob.AdMobConfig;
import com.maxrave.simpmusic.ads.admob.AdMobInterstitialAdGuide;
import com.maxrave.simpmusic.ads.config.AdConfig;
import com.maxrave.simpmusic.ads.nativead.AppNativeAdView;
import com.maxrave.simpmusic.utils.SharedPrefsHelper;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.ArrayList;
import java.util.List;


public class GuideActivity extends AppCompatActivity {

    private ViewPager2 pager;

    int currentPosition;

    AppNativeAdView templateView;

    ShimmerFrameLayout shimmerFrameLayout;

    RelativeLayout next_btn;

    ProgressBar progressBar;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    CardView loadingProgress;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_guide);


        Intent intent = getIntent();
        String type = intent.getStringExtra("type");

        templateView = findViewById(R.id.template_view);
        shimmerFrameLayout = findViewById(R.id.shimmer_native_small);
        next_btn = findViewById(R.id.progress_layout);
        progressBar = findViewById(R.id.progress_bar);
        loadingProgress = findViewById(R.id.progress);


        loadAdsData();


        pager = findViewById(R.id.guide_pager);
        DotsIndicator indicator = findViewById(R.id.dots_indicator);

        GuideAdapter adapter = new GuideAdapter(this, getList());
        pager.setAdapter(adapter);

        indicator.setViewPager2(pager);
        pager.registerOnPageChangeCallback(callback);

        sharedPreferences = getSharedPreferences("langSelection", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        next_btn.setOnClickListener(new View.OnClickListener() {
            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (pager.getCurrentItem() == 2) {
                            loadingProgress.setVisibility(View.VISIBLE);
                            // Preload necessary data for MainActivity here
                            AdMobInterstitialAdGuide.getInstance().showInterstitialAd(GuideActivity.this, new AdMobInterstitialAdGuide.AdClosedListener() {
                                @Override
                                public void onAdClosed() {
                                    new Handler().postDelayed(() -> {
                                        startActivity(new Intent(GuideActivity.this, MainActivity.class));
                                        editor.putBoolean("lang", false);
                                        editor.apply();
                                    }, 100);
                                }
                            });
                        } else {
                            loadingProgress.setVisibility(View.GONE);
                            pager.setCurrentItem(pager.getCurrentItem() + 1);
                        }
                    }
                });

            }
        });


    }

    ViewPager2.OnPageChangeCallback callback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            int totalPages = 3; // Update dynamically if the page count changes
            progressBar.setProgress((int) ((position + 1) / (float) totalPages * 100));
            currentPosition = position;
        }
    };

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            // Unregister any callbacks or listeners
            if (pager != null) {
                pager.unregisterOnPageChangeCallback(callback);
            }
            // Avoid setting visibility on views during onDestroy
            if (loadingProgress != null) {
                loadingProgress.setVisibility(View.GONE);
            }
        } catch (RuntimeException e) {
            if (e.getCause() instanceof android.os.DeadObjectException) {
                // Log and ignore the error if it's due to a DeadObjectException
                Log.w("GuideActivity", "Ignoring DeadObjectException during onDestroy", e);
            } else {
                throw e;
            }
        }

    }

    private List<GuideModel> getList() {
        List<GuideModel> list = new ArrayList<>();
        list.add(new GuideModel("Sing Along with Your \nFavorites", "Access and enjoy lyrics for all your favorite songs, enhancing your music experience.", R.drawable.sc1));
        list.add(new GuideModel("Discover Your Next Favorite\nSong", "Search and discover new tracks in seconds with our powerful music search engine.", R.drawable.sc2));
        list.add(new GuideModel("Instant Music Downloads", "Download your favorite songs instantly and store them directly on your phone.", R.drawable.sc3));
        return list;
    }

    private void loadAdsData() {
        AdMobInterstitialAdGuide.getInstance().init(this);
        View customeNativeView = findViewById(R.id.custom_template_view);
        AdConfig adConfig = (AdConfig) SharedPrefsHelper.loadObjectPrefs(this, SharedPrefsHelper.Key.Native_Guide.name(), AdConfig.class);
        if (adConfig != null && adConfig.Show) {
            AdMobConfig.showNativeAdWithLoad(this, templateView, adConfig.Ad_Id, shimmerFrameLayout, customeNativeView, "small");
        } else {
            templateView.setVisibility(View.GONE);
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        }
    }


}
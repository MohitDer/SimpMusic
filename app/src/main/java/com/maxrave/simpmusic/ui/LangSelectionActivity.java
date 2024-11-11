package com.maxrave.simpmusic.ui;



import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.adapter.LangAdapter;
import com.maxrave.simpmusic.adapter.LangModel;
import com.maxrave.simpmusic.ads.admob.AdMobConfig;
import com.maxrave.simpmusic.ads.config.AdConfig;
import com.maxrave.simpmusic.ads.nativead.AppNativeAdView;
import com.maxrave.simpmusic.utils.LanguagePreference;
import com.maxrave.simpmusic.utils.SharedPrefsHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;




public class LangSelectionActivity extends AppCompatActivity {

    List<LangModel> langList = new ArrayList<>();
    LangAdapter adapter;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    public AdLoader adLoader;
    String idNative;
    String idInter;
    boolean Show;
    boolean ShowInter;
    RecyclerView rvLang;
    ImageView ivDone;
    AppNativeAdView nativeTemplete;
    ShimmerFrameLayout shimmerFrameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lang_selection);

        MobileAds.initialize(this);

        rvLang = findViewById(R.id.rv_Lang);
        ivDone = findViewById(R.id.iv_done);
        nativeTemplete = findViewById(R.id.template_view);
        shimmerFrameLayout = findViewById(R.id.shimmer_native_small);

        loadAdsData();

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        sharedPreferences = getSharedPreferences("langSelection", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        langList.add(new LangModel(R.drawable.us, "English", "en"));
        langList.add(new LangModel(R.drawable.germany, "Deutschland", "de"));
        langList.add(new LangModel(R.drawable.spain, "España", "es"));
        langList.add(new LangModel(R.drawable.indonesia, "Indonesia", "in"));
        langList.add(new LangModel(R.drawable.italy, "Italia", "it"));
        langList.add(new LangModel(R.drawable.portugal, "portugal ", "pt"));
        langList.add(new LangModel(R.drawable.thailand, "ประเทศไทย", "th"));
        langList.add(new LangModel(R.drawable.vietnam, "Viêt Nam", "vi"));



        // Move the device's default language to the first position
        Locale defaultLocale = Locale.getDefault();
        int defaultLangIndex = -1;
        for (int i = 0; i < langList.size(); i++) {
            if (langList.get(i).getCode().equals(defaultLocale.getLanguage())) {
                defaultLangIndex = i;
                break;
            }
        }
        if (defaultLangIndex != -1) {
            LangModel defaultLang = langList.remove(defaultLangIndex);
            langList.add(0, defaultLang);
        }

        if (langList != null) {
            adapter = new LangAdapter(LangSelectionActivity.this, langList);
        }

        rvLang.setHasFixedSize(true);
        rvLang.setLayoutManager(new LinearLayoutManager(this));
        rvLang.setAdapter(adapter);

        ivDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LangModel selectedLang = adapter.getSelectedLang();
                if (selectedLang != null) {
                    editor.putBoolean("lang", false);
                    editor.apply();

                    // Save the selected language preference
//                    LanguagePreference.setSelectedLanguage(LangSelectionActivity.this, selectedLang.getCode());
//                    setAppLanguage(LangSelectionActivity.this, selectedLang.getCode());

                    Intent i = new Intent(LangSelectionActivity.this, GuideActivity.class);
                    i.putExtra("type", "splash");
                    startActivity(i);
                    finish();

                    Toast.makeText(LangSelectionActivity.this, "Selected Language: " + selectedLang.getLang(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LangSelectionActivity.this, "No language selected", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadAdsData() {
        View customeNativeView = findViewById(R.id.custom_template_view);

        AdConfig adConfig = (AdConfig) SharedPrefsHelper.loadObjectPrefs(this, SharedPrefsHelper.Key.Native_Lang.name(), AdConfig.class);
        if (adConfig != null && adConfig.Show) {
            AdMobConfig.showNativeAdWithLoad(this, nativeTemplete, adConfig.Ad_Id, shimmerFrameLayout, customeNativeView, "small");
        } else {
            nativeTemplete.setVisibility(View.GONE);
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        }
    }
}
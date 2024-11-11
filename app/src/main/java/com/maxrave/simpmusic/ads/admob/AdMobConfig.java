package com.maxrave.simpmusic.ads.admob;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.ads.nativead.AppNativeAdView;
import com.maxrave.simpmusic.ads.nativead.NativeAdStyle;

import java.lang.ref.WeakReference;

public class AdMobConfig {

    public static void showBannerAd(AdView adView) {
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                adView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Code to be executed when an ad request fails.
                adView.setVisibility(View.GONE);
            }
        });
        adView.loadAd(adRequest);
    }

    public static void showRectangleBannerAd(LinearLayout adViewContainer, AdView adView) {
        adViewContainer.removeAllViews();

        Bundle extras = new Bundle();
        extras.putString("collapsible", "top");

        AdRequest adRequest = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                .build();

        adView.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                adViewContainer.addView(adView);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Code to be executed when an ad request fails.
                adViewContainer.removeAllViews();
            }
        });
        adView.loadAd(adRequest);
    }

    public static void pause(AdView adView) {
        if (adView != null) {
            adView.pause();
        }
    }

    public static void resume(AdView adView) {
        if (adView != null) {
            adView.resume();
        }
    }

    public static void destroy(AdView adView) {
        if (adView != null) {
            adView.destroy();
            adView.setVisibility(View.GONE);
        }
    }

    public static void destroyRectangleBannerAd(LinearLayout adViewContainer, AdView adView) {
        if (adView != null) {
            adView.destroy();
            adViewContainer.removeAllViews();
        }
    }

    public static void showNativeAd(final Context context, AppNativeAdView nativeAdView, String nativeAdId) {
        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();
        String adId = nativeAdId;
        AdLoader adLoader = new AdLoader.Builder(context, adId)
                .forNativeAd(nativeAd -> {
                    NativeAdStyle styles = new NativeAdStyle.Builder().build();
                    nativeAdView.setStyles(styles);
                    nativeAdView.setNativeAd(nativeAd);
                }).withAdListener(new AdListener() {

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        nativeAdView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        nativeAdView.setVisibility(View.VISIBLE);
                    }
                }).withNativeAdOptions(adOptions).build();

        AdRequest.Builder builder = new AdRequest.Builder();
        adLoader.loadAd(builder.build());
    }

    public static void showNativeAdWithLoad(final Context context, AppNativeAdView nativeAdView, String nativeAdId, ShimmerFrameLayout shimmerFrameLayout, View customeNativeView, String type) {
        shimmerFrameLayout.startShimmer();
        nativeAdView.setVisibility(View.GONE);
        customeNativeView.setVisibility(View.GONE);
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();
        String adId = nativeAdId;
        AdLoader adLoader = new AdLoader.Builder(context, adId)
                .forNativeAd(nativeAd -> {
                    NativeAdStyle styles = new NativeAdStyle.Builder().build();
                    nativeAdView.setStyles(styles);
                    nativeAdView.setNativeAd(nativeAd);
                }).withAdListener(new AdListener() {

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        if (type.contains("small")) {
                            showSmallCustomeNativeAdWithLoad(context, shimmerFrameLayout, customeNativeView, type);
                        } else {
                            showCustomeNativeAdWithLoad(context, shimmerFrameLayout, customeNativeView, type);
                        }

                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        nativeAdView.setVisibility(View.VISIBLE);
                        shimmerFrameLayout.stopShimmer();
                        shimmerFrameLayout.setVisibility(View.GONE);
                    }
                }).withNativeAdOptions(adOptions).build();

        AdRequest.Builder builder = new AdRequest.Builder();
        adLoader.loadAd(builder.build());
    }

    private static void showSmallCustomeNativeAdWithLoad(Context context, ShimmerFrameLayout shimmerFrameLayout, View customeNativeView, String type) {
        final WeakReference<Context> weakContext = new WeakReference<>(context);
        FirebaseApp.initializeApp(context);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Ads");
        DatabaseReference bannerReference = databaseReference.child("Custome_Native_Small");
        bannerReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Context context = weakContext.get();
                if (context == null || (context instanceof Activity && (((Activity) context).isDestroyed() || ((Activity) context).isFinishing()))) {
                    return;
                }
                String titleText = dataSnapshot.child("Title_Text").getValue(String.class);
                String DisText = dataSnapshot.child("Description_Text").getValue(String.class);
                String Media_Url = dataSnapshot.child("Media_Url").getValue(String.class);
                String Action_Url = dataSnapshot.child("Action_Url").getValue(String.class);
                String Btn_Text = dataSnapshot.child("Btn_Text").getValue(String.class);
                Boolean showValue = dataSnapshot.child("Show").getValue(Boolean.class);


                if (showValue == true) {

                    customeNativeView.setVisibility(View.VISIBLE);
                    ImageView iv_Media = customeNativeView.findViewById(R.id.iv_Media_url);
                    TextView tv_Title = customeNativeView.findViewById(R.id.tv_title_text);
                    TextView tv_Disc = customeNativeView.findViewById(R.id.tv_discrption_text);
                    TextView tv_ads = customeNativeView.findViewById(R.id.tv_ads);
                    Button btn_action = customeNativeView.findViewById(R.id.btn_action);


                    tv_ads.bringToFront();


                    Glide.with(context)
                            .load(Media_Url)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    shimmerFrameLayout.stopShimmer();
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    shimmerFrameLayout.stopShimmer();
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    return false;
                                }
                            })
                            .into(iv_Media);

                    tv_Title.setText(titleText);
                    tv_Disc.setText(DisText);
                    btn_action.setText(Btn_Text);
                    btn_action.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Action_Url));
                            context.startActivity(browserIntent);
                        }
                    });

                } else {
                    customeNativeView.setVisibility(View.GONE);
                }





            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
            }
        });
    }

    private static void showCustomeNativeAdWithLoad(Context context, ShimmerFrameLayout shimmerFrameLayout, View customeNativeView, String type) {
        final WeakReference<Context> weakContext = new WeakReference<>(context);
        FirebaseApp.initializeApp(context);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Ads");
        DatabaseReference bannerReference = databaseReference.child("Custome_Native_Medium");
        bannerReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Context context = weakContext.get();
                if (context == null || (context instanceof Activity && (((Activity) context).isDestroyed() || ((Activity) context).isFinishing()))) {
                    return;
                }
                String titleText = dataSnapshot.child("Title_Text").getValue(String.class);
                String DisText = dataSnapshot.child("Description_Text").getValue(String.class);
                String Media_Url = dataSnapshot.child("Media_Url").getValue(String.class);
                String Icon_Url = dataSnapshot.child("Icon_Url").getValue(String.class);
                String Action_Url = dataSnapshot.child("Action_Url").getValue(String.class);
                String Btn_Text = dataSnapshot.child("Btn_Text").getValue(String.class);
                Boolean showValue = dataSnapshot.child("Show").getValue(Boolean.class);
                TextView tv_ads = customeNativeView.findViewById(R.id.tv_ads);

                if (showValue == true) {

                    customeNativeView.setVisibility(View.VISIBLE);
                    ImageView iv_Media = customeNativeView.findViewById(R.id.iv_Media_url);
                    ImageView iv_Icon = customeNativeView.findViewById(R.id.iv_Icon);
                    TextView tv_Title = customeNativeView.findViewById(R.id.tv_title_text);
                    TextView tv_Disc = customeNativeView.findViewById(R.id.tv_discrption_text);
                    Button btn_action = customeNativeView.findViewById(R.id.btn_action);

                    tv_ads.bringToFront();

                    Glide.with(context).load(Icon_Url).listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    }).into(iv_Icon);
                    Glide.with(context)
                            .load(Media_Url)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    shimmerFrameLayout.stopShimmer();
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    shimmerFrameLayout.stopShimmer();
                                    shimmerFrameLayout.setVisibility(View.GONE);
                                    return false;
                                }
                            })
                            .into(iv_Media);

                    tv_Title.setText(titleText);
                    tv_Disc.setText(DisText);
                    btn_action.setText(Btn_Text);
                    btn_action.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Action_Url));
                            context.startActivity(browserIntent);
                        }
                    });

                }else {
                    customeNativeView.setVisibility(View.GONE);
                }


                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);
            }
        });
    }

    public static void destroyNativeAd(AppNativeAdView nativeAdView) {
        if (nativeAdView != null) {
            nativeAdView.destroyNativeAd();
            nativeAdView.setVisibility(View.GONE);
        }
    }
}

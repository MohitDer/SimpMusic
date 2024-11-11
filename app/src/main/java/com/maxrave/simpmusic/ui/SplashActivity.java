package com.maxrave.simpmusic.ui;

import static com.maxrave.simpmusic.utils.Utils.isOnline;
import static com.maxrave.simpmusic.utils.Utils.showUpdateDialog;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.maxrave.simpmusic.App;
import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.databinding.ActivitySplashBinding;
import com.maxrave.simpmusic.utils.Utils;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long COUNTER_TIME = 3;

    private ActivitySplashBinding binding;

    public static String newAppPackageName;

    public static boolean isAppLive;

    public static int versionCode;

    int appVersionCode;

    boolean isLang = true;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("langSelection", MODE_PRIVATE);
        isLang = sharedPreferences.getBoolean("lang", true);

        appVersionCode = Utils.getAppVersionCode(getApplicationContext());

        // Create a timer so the SplashActivity will be displayed for a fixed amount of time.
//        if (isOnline(this)) {
//            FirebaseApp.initializeApp(this);
//            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Ads");
//            DatabaseReference UpdateReference = databaseReference.child("Update_Dialog");
//            UpdateReference.addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                    int version = dataSnapshot.child("versionCode").getValue(Integer.class);
//                    String Package = dataSnapshot.child("newAppPackageName").getValue(String.class);
//                    Boolean isLive = dataSnapshot.child("isAppLive").getValue(Boolean.class);
//
//                    if (version != -1) {
//                        versionCode = version;
//                    }
//
//                    if (Package != null) {
//                        newAppPackageName = Package;
//                    }
//
//                    if (isLive != null) {
//                        isAppLive = isLive.booleanValue();
//                    }
//
//                    if (appVersionCode < versionCode) {
//
//                        showUpdateDialog(SplashActivity.this);
//
//                    } else {
//                        createTimer(COUNTER_TIME);
//                    }
//
//
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError databaseError) {
//                    Log.e("FirebaseError", "Error reading Banner ad data from Firebase", databaseError.toException());
//                    createTimer(COUNTER_TIME);
//                }
//            });
//        } else {
            createTimer(COUNTER_TIME);
//        }

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            binding.version.setText(String.format("%s: v%s", getString(R.string.version), packageInfo.versionName));
            binding.version.setVisibility(View.VISIBLE);
        } catch (PackageManager.NameNotFoundException e) {
            binding.version.setVisibility(View.GONE);
        }
    }

    /**
     * Create the countdown timer, which counts down to zero and show the app open ad.
     *
     * @param seconds the number of seconds that the timer counts down from
     */
    private void createTimer(long seconds) {
        CountDownTimer countDownTimer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onFinish() {
                Application application = getApplication();

                // If the application is not an instance of App, start the MainActivity without showing the app open ad.
                if (!(application instanceof App)) {
                    if (isLang) {
                        Intent intent = new Intent(getApplicationContext(), GuideActivity.class);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                    }
                    return;
                }

                // Show the app open ad.
                ((App) application).showAdIfAvailable(SplashActivity.this, () ->{
                    if (isLang) {
                        Intent intent = new Intent(getApplicationContext(), GuideActivity.class);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                    }
                });
            }
        };
        countDownTimer.start();
    }



}
package com.maxrave.simpmusic.downloader.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {

    public static final int DOWNLOAD_DIALOG_REQUEST_CODE = 9001;
    public static final int DOWNLOADS_REQUEST_CODE = 9002;

    public static boolean checkStoragePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            return checkWriteStoragePermissions(activity, requestCode);
        } else {
            return checkReadStoragePermissions(activity, requestCode);
        }
    }

    public static boolean checkReadStoragePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED ||  ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.POST_NOTIFICATIONS}, requestCode);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
                return false;
            }
        }
        return true;
    }

    public static boolean checkWriteStoragePermissions(Activity activity, int requestCode) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            /*if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {*/

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);

            // PERMISSION_WRITE_STORAGE is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            /*}*/
            return false;
        }
        return true;
    }
}

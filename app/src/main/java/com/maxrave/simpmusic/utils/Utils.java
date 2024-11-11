package com.maxrave.simpmusic.utils;

import static com.maxrave.simpmusic.ui.SplashActivity.isAppLive;
import static com.maxrave.simpmusic.ui.SplashActivity.newAppPackageName;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.cardview.widget.CardView;
import androidx.media3.common.util.UnstableApi;

import com.annimon.stream.Optional;
import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.ui.MainActivity;

import java.util.Objects;

public class Utils {

    static Dialog updateDialog;
    static Dialog loadingDialog;


    public static boolean isOnline(@NonNull Context context) {
        return Boolean.TRUE.equals(Optional.ofNullable(((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))).map(ConnectivityManager::getActiveNetworkInfo).map(NetworkInfo::isConnected).orElse(false));
    }

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        try {
            if (uri.getScheme().equals("content")) {
                try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        final int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (columnIndex > -1) {
                            result = cursor.getString(columnIndex);
                        }
                    }
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
            if (result.indexOf(".") > 0)
                result = result.substring(0, result.lastIndexOf("."));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @OptIn(markerClass = UnstableApi.class)
    public static void showUpdateDialog(Activity context){

        updateDialog = new Dialog(context);

        updateDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        updateDialog.setContentView(R.layout.item_update_dialog);

        updateDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        updateDialog.show();

        CardView cv_update = updateDialog.findViewById(R.id.cv_update);

        CardView cv_cancel = updateDialog.findViewById(R.id.cv_cancelUpdate);

        if (isAppLive == true){
            updateDialog.setCancelable(true);
        }else{
            cv_cancel.setVisibility(View.GONE);
            updateDialog.setCancelable(false);
        }
        cv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateDialog.dismiss();
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
                context.finish();
            }
        });

        cv_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isAppLive == true){
                    lounchUpdate(context);
                }else{
                    lunchNewApp(context);
                }
            }
        });

    }

    @OptIn(markerClass = UnstableApi.class)
    public static void showLoadingDialog(Activity context){

        loadingDialog = new Dialog(context);

        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        loadingDialog.setContentView(R.layout.item_loading_dialog);

        loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        loadingDialog.show();
    }

    @OptIn(markerClass = UnstableApi.class)
    public static void closeLoadingDialog(){
        if (loadingDialog != null && loadingDialog.isShowing()){
            loadingDialog.dismiss();
        }
    }


    private static void lunchNewApp(Context context) {
        updateDialog.dismiss();
        try{
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+newAppPackageName)));
        }
        catch (ActivityNotFoundException e){
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id="+newAppPackageName)));
        }
    }

    private static void lounchUpdate(Context context) {
        updateDialog.dismiss();
        try{
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+context.getPackageName())));
        }
        catch (ActivityNotFoundException e){
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id="+context.getPackageName())));
        }
    }

    public static int getAppVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void showRateDialog(Context context){

        Dialog dialogRating = new Dialog(context);

        dialogRating.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Objects.requireNonNull(dialogRating.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

        dialogRating.setContentView(R.layout.item_dialog_rating);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialogRating.getWindow().getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        dialogRating.getWindow().setAttributes(layoutParams);

        dialogRating.getWindow().setGravity(Gravity.BOTTOM);

        Button rate = dialogRating.findViewById(R.id.btn_rate);

        ImageView cancel = dialogRating.findViewById(R.id.iv_cancel_rate);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogRating.isShowing()){
                    dialogRating.cancel();
                    dialogRating.dismiss();
                }

            }
        });

        rate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try{
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+context.getPackageName())));
                }
                catch (ActivityNotFoundException e){
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id="+context.getPackageName())));
                }
                Toast.makeText(context, "Thanks For Rating", Toast.LENGTH_SHORT).show();
                dialogRating.cancel();
                dialogRating.dismiss();
                if (dialogRating.isShowing()){
                    dialogRating.cancel();
                    dialogRating.dismiss();
                }
            }
        });

        dialogRating.show();


    }

}

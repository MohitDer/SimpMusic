package com.maxrave.simpmusic.downloader.util;

import android.content.Context;
import android.content.DialogInterface;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DialogUtils {

    public static void show(Context context,
                            String title,
                            String message,
                            String positiveTextButton,
                            DialogInterface.OnClickListener positiveListener,
                            String negativeTextButton,
                            DialogInterface.OnClickListener negativeListener) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveTextButton, positiveListener)
                .setNegativeButton(negativeTextButton, negativeListener)
                .show();
    }
}

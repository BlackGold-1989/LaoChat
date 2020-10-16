package com.laodev.chatapp.utils;

import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.laodev.chatapp.R;

public class BannerUtil {

    public static void onShowSuccessAlertEvent(View view, String alert, int milliseconds) {
        Snackbar.make(view, alert, milliseconds)
                .setBackgroundTint(view.getContext().getColor(R.color.green))
                .setActionTextColor(view.getContext().getColor(R.color.textColorWhite)).show();
    }

    public static void onShowSuccessAlertEvent(View view, int id, int milliseconds) {
        Snackbar.make(view, view.getContext().getString(id), milliseconds)
                .setBackgroundTint(view.getContext().getColor(R.color.green))
                .setActionTextColor(view.getContext().getColor(R.color.textColorWhite)).show();
    }

    public static void onShowErrorAlertEvent(View view, String alert, int milliseconds) {
        Snackbar.make(view, alert, milliseconds)
                .setBackgroundTint(view.getContext().getColor(R.color.red))
                .setActionTextColor(view.getContext().getColor(R.color.textColorWhite)).show();
    }

    public static void onShowErrorAlertEvent(View view, int id, int milliseconds) {
        Snackbar.make(view, view.getContext().getString(id), milliseconds)
                .setBackgroundTint(view.getContext().getColor(R.color.red))
                .setActionTextColor(view.getContext().getColor(R.color.textColorWhite)).show();
    }

    public static void onShowWaringAlertEvent(View view, String alert, int milliseconds) {
        Snackbar.make(view, alert, milliseconds)
                .setBackgroundTint(view.getContext().getColor(R.color.orange))
                .setActionTextColor(view.getContext().getColor(R.color.textColorWhite)).show();
    }

    public static void onShowWaringAlertEvent(View view, int id, int milliseconds) {
        Snackbar.make(view, view.getContext().getString(id), milliseconds)
                .setBackgroundTint(view.getContext().getColor(R.color.orange))
                .setActionTextColor(view.getContext().getColor(R.color.textColorWhite)).show();
    }

    public static void onShowProcessingAlertEvent(View view, String alert, int milliseconds) {
        Snackbar.make(view, alert, milliseconds)
                .setBackgroundTint(view.getContext().getColor(R.color.blue))
                .setActionTextColor(view.getContext().getColor(R.color.textColorWhite)).show();
    }
    
}

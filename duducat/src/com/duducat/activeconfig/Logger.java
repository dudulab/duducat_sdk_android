package com.duducat.activeconfig;

import android.util.Log;

class Logger {

    private static final String TAG = "Duducat";

    public static int i(String msg) {
        return Log.i(TAG, msg);
    }

    public static int e(String msg) {
        return Log.e(TAG, msg);
    }

    public static int w(String msg) {
        return Log.w(TAG, msg);
    }

    public static int e(String msg, Throwable tr) {
        return Log.e(TAG, msg, tr);
    }
}

package com.topjohnwu.magisk.utils;

import android.util.Log;

import com.topjohnwu.magisk.Global;

public class Logger {

    public static final String TAG = "Magisk";

    public static void debug(String msg) {
        Log.d(TAG, "DEBUG: " + msg);
    }

    public static void error(String msg) {
        Log.e(TAG, "ERROR: " + msg);
    }

    public static void dev(String msg, Object... args) {
        if (Global.Configs.devLogging) {
            if (args.length == 1 && args[0] instanceof Throwable) {
                Log.d(TAG, "DEV: " + msg, (Throwable) args[0]);
            } else {
                Log.d(TAG, "DEV: " + String.format(msg, args));
            }
        }
    }

    public static void dev(String msg) {
        if (Global.Configs.devLogging) {
            Log.d(TAG, "DEBUG: " + msg);
        }
    }

    public static void shell(boolean root, String msg) {
        if (Global.Configs.shellLogging) {
            Log.d(root ? "SU" : "SH", msg);
        }
    }
}

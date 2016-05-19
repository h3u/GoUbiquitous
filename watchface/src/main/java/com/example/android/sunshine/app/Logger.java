package com.example.android.sunshine.app;

import android.util.Log;

/**
 * Created by Uli Wucherer (u.wucherer@gmail.com) on 10/05/16.
 */
public class Logger {

    public static void debug(final String tag, String message) {
        if (BuildConfig.DEBUG || Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }

    public static void error(final String tag, String message, Throwable throwable) {
        if (Log.isLoggable(tag, Log.ERROR)) {
            Log.e(tag, message, throwable);
        }
    }

    public static void error(final String tag, String message) {
        Logger.error(tag, message, null);
    }
}

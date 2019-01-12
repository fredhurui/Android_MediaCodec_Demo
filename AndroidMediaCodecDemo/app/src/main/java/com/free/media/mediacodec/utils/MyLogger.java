package com.free.media.mediacodec.utils;

import android.util.Log;

/**
 * A log helper class, the log.v and log.d is disabled by default
 * it can be enabled by set property, for example to enable debug log
 * adb shell setprop log.tag.MyCodecDemo DEBUG
 * to enable verbose log
 * adb shell setprop log.tag.MyCodecDemo DEBUG
 * to disable debug log
 * adb shell setprop log.tag.MyCodecDemo INFO
 */
public class MyLogger {
    private static final String PREFIX = "MyCodecDemo";
    
    public static void v(String TAG, String message) {
        if(Log.isLoggable(PREFIX, Log.VERBOSE)) {
            Log.v(PREFIX, TAG + ":" + message);
        }
        
    }
    
    public static void d(String TAG, String message) {
        if(Log.isLoggable(PREFIX, Log.DEBUG)) {
            Log.d(PREFIX, TAG + ":" + message);
        }
    }
    
    public static void i(String TAG, String message) {
        Log.i(PREFIX, TAG + ":" + message);
    }
    
    public static void w(String TAG, String message) {
        Log.w(PREFIX, TAG + ":" + message);
    }
    
    public static void e(String TAG, String message) {
        Log.e(PREFIX, TAG + ":" + message);
    }
}

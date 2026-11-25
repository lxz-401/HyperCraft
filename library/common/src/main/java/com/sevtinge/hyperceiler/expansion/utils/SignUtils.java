package com.sevtinge.hyperceiler.expansion.utils;

import android.content.Context;

public class SignUtils {
    public static boolean isSignCheckPass(Context context) {
        return true;
    }
    public static String getSignType(Context context) {
        return "Official";
    }
    public static String getSHA256Signature(Context context) {
        return "hypercraft_signature";
    }
}

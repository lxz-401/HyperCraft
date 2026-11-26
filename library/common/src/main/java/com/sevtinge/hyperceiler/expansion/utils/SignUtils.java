package com.sevtinge.hyperceiler.expansion.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;

import java.security.MessageDigest;

public class SignUtils {
    // HyperCraft supports local signing keys. PackageManager validates the installed
    // APK; this checks certificate availability, not an upstream certificate allowlist.
    public static boolean isSignCheckPass(Context context) {
        return !getSHA256Signature(context).isEmpty();
    }

    public static String getSignType(Context context) {
        return isSignCheckPass(context) ? "Local" : "Unknown";
    }

    public static String getSHA256Signature(Context context) {
        if (context == null) {
            return "";
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                context.getPackageName(),
                PackageManager.GET_SIGNING_CERTIFICATES
            );
            SigningInfo signingInfo = packageInfo.signingInfo;
            Signature[] signatures = signingInfo == null
                ? null
                : signingInfo.getApkContentsSigners();
            if (signatures == null || signatures.length == 0) {
                return "";
            }
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray());
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >>> 4) & 0x0F, 16));
                hex.append(Character.forDigit(b & 0x0F, 16));
            }
            return hex.toString();
        } catch (Exception ignored) {
            return "";
        }
    }
}

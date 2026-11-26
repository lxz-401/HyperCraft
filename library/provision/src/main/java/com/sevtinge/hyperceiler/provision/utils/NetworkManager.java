package com.sevtinge.hyperceiler.provision.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkManager {

    public static boolean isNetworkConnected(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        Network network = cm.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities nc = cm.getNetworkCapabilities(network);
            if (nc != null) {
                if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true;
                } else return nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        }
        return false;
    }

    public static boolean isInternetAvailable() {
        try {
            InetAddress address = InetAddress.getByName("cloudflare.com");
            return !address.isAnyLocalAddress() && !address.isLoopbackAddress();
        } catch (UnknownHostException e) {
            // Log error
        }
        return false;
    }

}

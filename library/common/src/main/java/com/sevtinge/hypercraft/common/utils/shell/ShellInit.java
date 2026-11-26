package com.sevtinge.hypercraft.common.utils.shell;

import com.sevtinge.hypercraft.common.log.AndroidLog;

public class ShellInit {
    private final static String TAG = "ShellInit";
    private static ShellExec mShell = null;
    private static boolean lastReady = false;
    private static IResult mResult = null;

    public static void init() {
        init(null);
    }

    public static synchronized void init(IResult result) {
        try {
            if (mShell != null && !mShell.isDestroy()) {
                return;
            }
            mResult = result;
            mShell = new ShellExec(true, true, result);
            lastReady = mShell.ready();
        } catch (RuntimeException e) {
            AndroidLog.e(TAG, "init failed", e);
        }
    }

    public static synchronized void destroy() {
        if (mShell != null && !mShell.isDestroy()) {
            mShell.close();
            mShell = null;
            mResult = null;
        } else if (mShell != null && mShell.isDestroy()) {
            mShell = null;
            mResult = null;
        }
    }

    public static synchronized ShellExec getShell() {
        if (mShell != null) {
            if (!mShell.isRoot()) {
                return mShell;
            }
            if (mShell.isDestroy()) {
                AndroidLog.w(TAG, "The current shell has been destroyed, please try creating it again!");
                mShell = new ShellExec(true, true, mResult);
            }
            return mShell;
        } else {
            if (lastReady) {
                AndroidLog.w(TAG, "ShellExec is null!! Attempt to rewrite creation...");
            }
            mShell = new ShellExec(true, true, mResult);
            return mShell;
        }
    }

    public static synchronized boolean ready() {
        if (mShell != null) {
            if (!mShell.isRoot()) {
                return false;
            }
            if (mShell.isDestroy()) {
                init(mResult);
            }
            return mShell.ready();
        }
        if (lastReady) {
            init(mResult);
            return mShell.ready();
        }
        return false;
    }
}

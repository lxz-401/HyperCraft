/*
 * This file is part of HyperCraft.
 * Licensed under the GNU General Public License v3.0
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.plugin.systemui

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Keep view state on main and finish restoration before hot reload can proceed. */
internal fun runVolumeCleanupOnMain(cleanup: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        cleanup()
        return
    }
    val handler = Handler(Looper.getMainLooper())
    val task = FutureTask { cleanup() }
    check(handler.post(task)) { "Volume cleanup could not reach main thread" }
    try {
        task.get(500, TimeUnit.MILLISECONDS)
    } catch (error: TimeoutException) {
        task.cancel(false)
        handler.removeCallbacks(task)
        throw IllegalStateException("Volume cleanup has not completed", error)
    } catch (error: InterruptedException) {
        task.cancel(false)
        handler.removeCallbacks(task)
        Thread.currentThread().interrupt()
        throw IllegalStateException("Volume cleanup interrupted", error)
    } catch (error: ExecutionException) {
        throw IllegalStateException("Volume cleanup failed", error.cause)
    }
}

package com.sevtinge.hypercraft.libhook.rules.systemui.plugin.systemui

import android.os.Handler
import android.os.Looper
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

fun main() {
    val state = AtomicInteger(0)
    val worker = thread {
        runVolumeCleanupOnMain { state.set(1) }
        check(state.get() == 1) { "Cleanup returned before restoring views" }
        state.set(2)
    }
    val work = checkNotNull(Handler.pending.poll(2, TimeUnit.SECONDS))
    check(state.get() == 0 && worker.isAlive)
    Looper.enterMain()
    work.run()
    worker.join(2000)
    check(!worker.isAlive && state.get() == 2)

    runVolumeCleanupOnMain { state.set(3) }
    check(state.get() == 3 && Handler.pending.isEmpty())
    Looper.leaveMain()

    val original = IllegalArgumentException("restore failed")
    val failure = AtomicReference<Throwable>()
    val failedWorker = thread {
        try { runVolumeCleanupOnMain { throw original } }
        catch (error: Throwable) { failure.set(error) }
    }
    checkNotNull(Handler.pending.poll(2, TimeUnit.SECONDS)).run()
    failedWorker.join(2000)
    check(failure.get() is IllegalStateException && failure.get().cause === original)

    val ranAfterTimeout = AtomicBoolean(false)
    val timeout = runCatching { runVolumeCleanupOnMain { ranAfterTimeout.set(true) } }.exceptionOrNull()
    check(timeout is IllegalStateException && timeout.cause is java.util.concurrent.TimeoutException)
    check(Handler.pending.isEmpty() && !ranAfterTimeout.get())

    val interrupted = AtomicBoolean(false)
    val interruptedWorker = thread {
        try { runVolumeCleanupOnMain { error("Canceled work ran") } }
        catch (error: IllegalStateException) {
            check(error.cause is InterruptedException)
            interrupted.set(Thread.currentThread().isInterrupted)
        }
    }
    val queued = checkNotNull(Handler.pending.poll(2, TimeUnit.SECONDS))
    interruptedWorker.interrupt()
    interruptedWorker.join(2000)
    check(interrupted.get() && !interruptedWorker.isAlive)
    queued.run() // A canceled FutureTask must not execute its old-generation callback.

    Handler.accepting = false
    check(runCatching { runVolumeCleanupOnMain {} }.exceptionOrNull() is IllegalStateException)
    check(Handler.pending.isEmpty())
    println("Volume main cleanup regression checks passed")
}

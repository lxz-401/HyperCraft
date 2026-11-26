/* SPDX-License-Identifier: AGPL-3.0-or-later */
import android.os.Handler
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.rules.systemui.other.MonetThemeOverlay
import com.sevtinge.hyperceiler.libhook.utils.api.HostExecutor
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.Hooks
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private fun await(latch: CountDownLatch) = check(latch.await(3, TimeUnit.SECONDS)) { "worker timed out" }
fun main() {
    repeat(3) { generation ->
        val delivered = CountDownLatch(1)
        HostExecutor.execute("reload", { generation }, false) { check(it == generation); delivered.countDown() }
        await(delivered)
        check(BaseHook.cleanups.size == 1) { "one cleanup registration per generation" }
        BaseHook.cleanup()
    }
    println("PASS HostExecutor recreation after three cleanup cycles")
    val oldStarted = CountDownLatch(1)
    val oldInterrupted = CountDownLatch(1)
    val newDelivered = CountDownLatch(1)
    HostExecutor.execute("same", {
        oldStarted.countDown()
        try { CountDownLatch(1).await(); 1 } catch (e: InterruptedException) { oldInterrupted.countDown(); throw e }
    }, false) { error("cancelled task delivered a result") }
    await(oldStarted)
    HostExecutor.execute("same", { 2 }, false) { check(it == 2); newDelivered.countDown() }
    await(oldInterrupted); await(newDelivered)
    val posted = CountDownLatch(1)
    HostExecutor.execute("main", { posted.countDown(); 3 }, true) { error("retired main callback ran") }
    await(posted)
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
    while (Handler.pending.isEmpty() && System.nanoTime() < deadline) Thread.yield()
    check(Handler.pending.isNotEmpty())
    BaseHook.cleanup(); Handler.drain()
    println("PASS HostExecutor same-tag cancellation and retired main callback removal")
    BaseHook.classMissing = true
    MonetThemeOverlay().init()
    check(Hooks.registrations == 0) { "missing optional Monet class must be skipped" }
    BaseHook.classMissing = false
    MonetThemeOverlay().init()
    repeat(1000) { color ->
        PrefsBridge.values["system_ui_monet_overlay_custom_color"] = color
        val args = HookParam(0)
        Hooks.before!!(args)
        check(args.args[0] == color)
    }
    val empty = HookParam(); Hooks.before!!(empty)
    val unsupported = HookParam("different overload"); Hooks.before!!(unsupported)
    check(unsupported.args[0] == "different overload")
    check(Hooks.registrations == 1)
    println("PASS Monet optional-class/overload guards and 1000 live hot-path updates")
}

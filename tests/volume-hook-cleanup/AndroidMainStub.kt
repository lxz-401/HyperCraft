package android.os

import java.util.concurrent.LinkedBlockingQueue

class Looper {
    companion object {
        private val main = Looper()
        private val current = ThreadLocal<Looper>()
        fun getMainLooper() = main
        fun myLooper(): Looper? = current.get()
        fun enterMain() { current.set(main) }
        fun leaveMain() { current.remove() }
    }
}

class Handler(@Suppress("UNUSED_PARAMETER") looper: Looper) {
    companion object {
        val pending = LinkedBlockingQueue<Runnable>()
        var accepting = true
    }
    fun post(action: Runnable) = accepting && pending.offer(action)
    fun removeCallbacks(action: Runnable) { pending.remove(action) }
}

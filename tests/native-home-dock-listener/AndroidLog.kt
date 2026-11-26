package com.sevtinge.hypercraft.common.log
object ModuleLog {
    val lines = mutableListOf<String>()
    fun i(tag: String, message: String): Int { lines += message; return 0 }
}

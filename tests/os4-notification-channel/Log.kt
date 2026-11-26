package com.sevtinge.hypercraft.common.log
object XposedLog { fun w(tag: String, pkg: String, message: String, error: Throwable) { throw AssertionError(message, error) } }

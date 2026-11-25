package com.sevtinge.hyperceiler.expansion.utils

object TokenUtils {
    @JvmStatic
    fun getDeviceToken(modelName: String, serial: String, androidId: String, cpuId: String): String {
        return "HYPERCRAFT_" + modelName.hashCode()
    }
}

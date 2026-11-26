package com.sevtinge.hyperceiler.expansion.utils

import java.security.MessageDigest

object TokenUtils {
    @JvmStatic
    fun getDeviceToken(modelName: String, serial: String, androidId: String, cpuId: String): String {
        val payload = "$modelName|$serial|$androidId|$cpuId"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        val hex = StringBuilder(16)
        for (i in 0 until minOf(8, digest.size)) {
            val b = digest[i].toInt() and 0xFF
            hex.append(Character.forDigit(b ushr 4, 16))
            hex.append(Character.forDigit(b and 0x0F, 16))
        }
        return "HYPERCRAFT_$hex"
    }
}

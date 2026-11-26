package com.sevtinge.hypercraft.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit tests for the [ReflectionUtils.Result] type and its helper extensions.
 *
 * These exercise pure JVM logic (Result + standard reflection) only. The
 * HiddenApiBypass-backed variants (safeInvoke/safeGetField/safeSetField) depend
 * on android.os.Build and are covered by device/instrumented tests instead.
 */
class ReflectionUtilsTest {

    @Test
    fun resultSuccess_onSuccess_invokesAction() {
        val success = ReflectionUtils.Result.Success<String>("value")
        val sb = StringBuilder()
        success.onSuccess { sb.append(it) }
        assertEquals("value", sb.toString())
    }

    @Test
    fun resultSuccess_onFailure_notInvoked() {
        val success = ReflectionUtils.Result.Success<String>("value")
        success.onFailure { _, _ -> fail("onFailure should not be called on Success") }
    }

    @Test
    fun resultFailure_onFailure_invokesAction() {
        val ex = RuntimeException("test")
        val failure = ReflectionUtils.Result.Failure(ex, "message")
        val sb = StringBuilder()
        failure.onFailure { e, msg -> sb.append(msg).append(":").append(e.message) }
        assertEquals("message:test", sb.toString())
    }

    @Test
    fun resultFailure_onSuccess_notInvoked() {
        val failure = ReflectionUtils.Result.Failure(RuntimeException(), "msg")
        failure.onSuccess { _ -> fail("onSuccess should not be called on Failure") }
    }

    @Test
    fun result_getOrThrow_success_returnsValue() {
        val success = ReflectionUtils.Result.Success<String>("value")
        assertEquals("value", success.getOrThrow())
    }

    @Test(expected = RuntimeException::class)
    fun result_getOrThrow_failure_throws() {
        val failure = ReflectionUtils.Result.Failure(RuntimeException("test"), "msg")
        failure.getOrThrow()
    }

    @Test
    fun result_getOrDefault_success_returnsValue() {
        val success = ReflectionUtils.Result.Success<String>("value")
        assertEquals("value", success.getOrDefault("default"))
    }

    @Test
    fun result_getOrDefault_failure_returnsDefault() {
        val failure = ReflectionUtils.Result.Failure(RuntimeException("test"), "msg")
        assertEquals("default", failure.getOrDefault("default"))
    }

    @Test
    fun safeInvokeStandard_invokesPublicMethod() {
        // Pure JVM reflection: invoke String.length() with no args.
        val result = ReflectionUtils.safeInvokeStandard(
            clazz = String::class.java,
            receiver = "abc",
            methodName = "length",
            args = emptyArray()
        )
        assertEquals(3, result.getOrThrow())
    }

    @Test
    fun safeInvokeStandard_missingMethod_isFailure() {
        val result = ReflectionUtils.safeInvokeStandard(
            clazz = String::class.java,
            receiver = "abc",
            methodName = "noSuchMethod",
            args = emptyArray()
        )
        assertTrue(result is ReflectionUtils.Result.Failure)
        assertNull(null)
    }
}

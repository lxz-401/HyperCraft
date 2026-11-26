package com.sevtinge.hypercraft.common.utils

import android.os.Build
import com.sevtinge.hypercraft.common.log.XposedLog
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Safe wrapper for reflection and HiddenApiBypass calls.
 * All hidden API access should go through this utility to ensure:
 * - Proper exception handling
 * - Version guards
 * - Consistent logging
 * - Safe mode integration
 */
object ReflectionUtils {

    private const val TAG = "ReflectionUtils"

    /**
     * Result wrapper for safe reflection operations
     */
    sealed class Result<out T> {
        data class Success<out T>(val value: T) : Result<T>()
        data class Failure(val exception: Exception, val message: String) : Result<Nothing>()
    }

    /**
     * Safely invoke a method via HiddenApiBypass with version guard and fallback.
     *
     * @param clazz Target class
     * @param receiver Receiver object (null for static methods)
     * @param methodName Method name
     * @param args Method arguments
     * @param minSdk Minimum SDK version required (optional)
     * @param maxSdk Maximum SDK version allowed (optional)
     * @return Result containing the return value or failure
     */
    @Suppress("UNUSED_PARAMETER")
    fun safeInvoke(
        clazz: Class<*>,
        receiver: Any?,
        methodName: String,
        args: Array<Any?> = emptyArray(),
        minSdk: Int = Build.VERSION_CODES.BASE,
        maxSdk: Int = Build.VERSION_CODES.CUR_DEVELOPMENT
    ): Result<Any?> {
        // Version guard
        val sdk = Build.VERSION.SDK_INT
        if (sdk < minSdk || sdk > maxSdk) {
            return Result.Failure(
                IllegalStateException("SDK $sdk not in range [$minSdk, $maxSdk]"),
                "Version guard failed: SDK $sdk not in [$minSdk, $maxSdk] for $clazz.$methodName"
            )
        }

        return try {
            val result = HiddenApiBypass.invoke(clazz, receiver, methodName, *args)
            Result.Success(result)
        } catch (e: NoSuchMethodException) {
            logFailure(clazz, methodName, "Method not found", e)
            Result.Failure(e, "Method not found: $clazz.$methodName")
        } catch (e: IllegalAccessException) {
            logFailure(clazz, methodName, "Illegal access", e)
            Result.Failure(e, "Illegal access: $clazz.$methodName")
        } catch (e: Exception) {
            logFailure(clazz, methodName, "Invocation failed", e)
            Result.Failure(e, "Invocation failed: $clazz.$methodName - ${e.message}")
        }
    }

    /**
     * Safely invoke a static method via HiddenApiBypass.
     */
    fun safeInvokeStatic(
        clazz: Class<*>,
        methodName: String,
        args: Array<Any?> = emptyArray(),
        minSdk: Int = Build.VERSION_CODES.BASE,
        maxSdk: Int = Build.VERSION_CODES.CUR_DEVELOPMENT
    ): Result<Any?> = safeInvoke(clazz, null, methodName, args, minSdk, maxSdk)

    /**
     * Safely get a field value via HiddenApiBypass.
     */
    fun safeGetField(
        clazz: Class<*>,
        receiver: Any?,
        fieldName: String,
        minSdk: Int = Build.VERSION_CODES.BASE,
        maxSdk: Int = Build.VERSION_CODES.CUR_DEVELOPMENT
    ): Result<Any?> {
        val sdk = Build.VERSION.SDK_INT
        if (sdk < minSdk || sdk > maxSdk) {
            return Result.Failure(
                IllegalStateException("SDK $sdk not in range [$minSdk, $maxSdk]"),
                "Version guard failed for field $clazz.$fieldName"
            )
        }

        return try {
            val field = HiddenApiBypass.findField(clazz, fieldName) ?: throw NoSuchFieldException(fieldName)
            field.isAccessible = true
            Result.Success(field.get(receiver))
        } catch (e: NoSuchFieldException) {
            logFailure(clazz, fieldName, "Field not found", e)
            Result.Failure(e, "Field not found: $clazz.$fieldName")
        } catch (e: Exception) {
            logFailure(clazz, fieldName, "Field access failed", e)
            Result.Failure(e, "Field access failed: $clazz.$fieldName - ${e.message}")
        }
    }

    /**
     * Safely set a field value via HiddenApiBypass.
     */
    fun safeSetField(
        clazz: Class<*>,
        receiver: Any?,
        fieldName: String,
        value: Any?,
        minSdk: Int = Build.VERSION_CODES.BASE,
        maxSdk: Int = Build.VERSION_CODES.CUR_DEVELOPMENT
    ): Result<Unit> {
        val sdk = Build.VERSION.SDK_INT
        if (sdk < minSdk || sdk > maxSdk) {
            return Result.Failure(
                IllegalStateException("SDK $sdk not in range [$minSdk, $maxSdk]"),
                "Version guard failed for field $clazz.$fieldName"
            )
        }

        return try {
            val field = HiddenApiBypass.findField(clazz, fieldName) ?: throw NoSuchFieldException(fieldName)
            field.isAccessible = true
            field.set(receiver, value)
            Result.Success(Unit)
        } catch (e: NoSuchFieldException) {
            logFailure(clazz, fieldName, "Field not found", e)
            Result.Failure(e, "Field not found: $clazz.$fieldName")
        } catch (e: Exception) {
            logFailure(clazz, fieldName, "Field set failed", e)
            Result.Failure(e, "Field set failed: $clazz.$fieldName - ${e.message}")
        }
    }

    /**
     * Safely find and invoke a constructor via HiddenApiBypass.
     */
    fun safeNewInstance(
        clazz: Class<*>,
        args: Array<Any?> = emptyArray(),
        minSdk: Int = Build.VERSION_CODES.BASE,
        maxSdk: Int = Build.VERSION_CODES.CUR_DEVELOPMENT
    ): Result<Any?> {
        val sdk = Build.VERSION.SDK_INT
        if (sdk < minSdk || sdk > maxSdk) {
            return Result.Failure(
                IllegalStateException("SDK $sdk not in range [$minSdk, $maxSdk]"),
                "Version guard failed for constructor $clazz"
            )
        }

        return try {
            val constructors = clazz.declaredConstructors
            if (constructors.isEmpty()) throw NoSuchMethodException("No constructors found")
            // Try to find matching constructor
            val constructor = constructors.firstOrNull { it.parameterCount == args.size }
                ?: constructors.first()
            constructor.isAccessible = true
            Result.Success(constructor.newInstance(*args))
        } catch (e: Exception) {
            logFailure(clazz, "<init>", "Constructor invocation failed", e)
            Result.Failure(e, "Constructor failed: $clazz - ${e.message}")
        }
    }

    /**
     * Standard reflection fallback when HiddenApiBypass is not needed/available.
     */
    fun safeInvokeStandard(
        clazz: Class<*>,
        receiver: Any?,
        methodName: String,
        args: Array<Any?> = emptyArray()
    ): Result<Any?> {
        return try {
            val method = clazz.getDeclaredMethod(methodName, *args.map { it?.javaClass ?: Any::class.java }.toTypedArray())
            method.isAccessible = true
            Result.Success(method.invoke(receiver, *args))
        } catch (e: Exception) {
            logFailure(clazz, methodName, "Standard reflection failed", e)
            Result.Failure(e, "Standard reflection failed: $clazz.$methodName - ${e.message}")
        }
    }

    private fun logFailure(clazz: Class<*>, member: String, reason: String, e: Exception) {
        XposedLog.w(TAG, "$reason: $clazz.$member - ${e.message}", e)
        // TODO: Report to SafeModeHandler if critical
    }

    /**
     * Extension for easier chaining: onSuccess { value -> ... }
     */
    inline fun <T> Result<T>.onSuccess(crossinline action: (T) -> Unit): Result<T> {
        if (this is Result.Success) action(value)
        return this
    }

    /**
     * Extension for easier chaining: onFailure { exception, message -> ... }
     */
    inline fun <T> Result<T>.onFailure(crossinline action: (Exception, String) -> Unit): Result<T> {
        if (this is Result.Failure) action(exception, message)
        return this
    }

    /**
     * Get value or throw if failure (for cases where failure should crash)
     */
    fun <T> Result<T>.getOrThrow(): T = when (this) {
        is Result.Success -> value
        is Result.Failure -> throw exception
    }

    /**
     * Get value or default
     */
    fun <T> Result<T>.getOrDefault(default: T): T = when (this) {
        is Result.Success -> value
        is Result.Failure -> default
    }
}
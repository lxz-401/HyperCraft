package com.sevtinge.hyperceiler.common.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ReflectionUtilsTest {

    @Test
    public void testResultSuccess_onSuccess() {
        ReflectionUtils.Result.Success<String> success = new ReflectionUtils.Result.Success<>("value");
        StringBuilder sb = new StringBuilder();
        success.onSuccess(sb::append);
        assertEquals("value", sb.toString());
    }

    @Test
    public void testResultSuccess_onFailure_notCalled() {
        ReflectionUtils.Result.Success<String> success = new ReflectionUtils.Result.Success<>("value");
        success.onFailure((e, msg) -> fail("onFailure should not be called on Success"));
    }

    @Test
    public void testResultFailure_onFailure() {
        Exception ex = new RuntimeException("test");
        ReflectionUtils.Result.Failure failure = new ReflectionUtils.Result.Failure(ex, "message");
        StringBuilder sb = new StringBuilder();
        failure.onFailure((e, msg) -> {
            sb.append(msg).append(":").append(e.getMessage());
        });
        assertEquals("message:test", sb.toString());
    }

    @Test
    public void testResultFailure_onSuccess_notCalled() {
        ReflectionUtils.Result.Failure failure = new ReflectionUtils.Result.Failure(new RuntimeException(), "msg");
        failure.onSuccess(v -> fail("onSuccess should not be called on Failure"));
    }

    @Test
    public void testResult_getOrThrow_success() {
        ReflectionUtils.Result.Success<String> success = new ReflectionUtils.Result.Success<>("value");
        assertEquals("value", success.getOrThrow());
    }

    @Test(expected = RuntimeException.class)
    public void testResult_getOrThrow_failure() {
        ReflectionUtils.Result.Failure failure = new ReflectionUtils.Result.Failure(new RuntimeException("test"), "msg");
        failure.getOrThrow();
    }

    @Test
    public void testResult_getOrDefault_success() {
        ReflectionUtils.Result.Success<String> success = new ReflectionUtils.Result.Success<>("value");
        assertEquals("value", success.getOrDefault("default"));
    }

    @Test
    public void testResult_getOrDefault_failure() {
        ReflectionUtils.Result.Failure failure = new ReflectionUtils.Result.Failure(new RuntimeException("test"), "msg");
        assertEquals("default", failure.getOrDefault("default"));
    }
}
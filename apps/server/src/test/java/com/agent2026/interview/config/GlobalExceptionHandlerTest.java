package com.agent2026.interview.config;

import com.agent2026.interview.common.LlmApiException;
import com.agent2026.interview.common.Result;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionKeepsStableCodeAndSafeMessage() {
        BusinessException exception = new BusinessException(
                ErrorCode.PROJECT_PROFILE_STATE_CONFLICT,
                "当前项目状态不允许确认"
        );

        Result<Void> result = handler.handleBusinessException(exception);

        assertEquals(ErrorCode.PROJECT_PROFILE_STATE_CONFLICT.getCode(), result.getCode());
        assertEquals("当前项目状态不允许确认", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void blankBusinessMessageFallsBackToErrorCodeMessage() {
        BusinessException exception = new BusinessException(ErrorCode.PROJECT_PROFILE_NOT_FOUND, " ");

        Result<Void> result = handler.handleBusinessException(exception);

        assertEquals(ErrorCode.PROJECT_PROFILE_NOT_FOUND.getMessage(), result.getMsg());
    }

    @Test
    void llmExceptionKeepsExistingSafeCodeAndMessageContract() {
        LlmApiException exception = new LlmApiException(429, "大模型服务请求过于频繁，请稍后重试");

        Result<Void> result = handler.handleLlmApiException(exception);

        assertEquals(429, result.getCode());
        assertEquals("大模型服务请求过于频繁，请稍后重试", result.getMsg());
    }

    @Test
    void unexpectedExceptionDoesNotExposeInternalDetails() {
        RuntimeException exception = new RuntimeException("database password and SQL details");

        Result<Void> result = handler.handleException(exception);

        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), result.getCode());
        assertEquals(ErrorCode.INTERNAL_ERROR.getMessage(), result.getMsg());
        assertFalse(result.getMsg().contains("password"));
    }

    @Test
    void illegalStateDoesNotExposeInternalStateMessage() {
        IllegalStateException exception = new IllegalStateException("secret state machine details");

        Result<Void> result = handler.handleIllegalState(exception);

        assertEquals(ErrorCode.INTERNAL_ERROR.getCode(), result.getCode());
        assertEquals("服务状态异常，请稍后重试", result.getMsg());
        assertFalse(result.getMsg().contains("state machine"));
    }
}

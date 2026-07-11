package com.agent2026.interview.config;

import com.agent2026.interview.common.Result;
import com.agent2026.interview.common.LlmApiException;
import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "请求参数不合法" : error.getDefaultMessage())
                .orElse("请求参数不合法");
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), "请求体格式不合法");
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        return Result.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(LlmApiException.class)
    public Result<Void> handleLlmApiException(LlmApiException ex) {
        log.warn("LLM API request failed, upstreamCode={}", ex.getCode());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalState(IllegalStateException ex) {
        log.error("Unexpected service state", ex);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "服务状态异常，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("Unhandled server exception", ex);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
    }
}

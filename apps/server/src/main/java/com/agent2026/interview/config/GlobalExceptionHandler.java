package com.agent2026.interview.config;

import com.agent2026.interview.common.Result;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "request parameter invalid" : error.getDefaultMessage())
                .orElse("request parameter invalid");
        return Result.error(400, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return Result.error(400, "request body invalid");
    }

    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalState(IllegalStateException ex) {
        return Result.error(500, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        return Result.error(500, "server error");
    }
}

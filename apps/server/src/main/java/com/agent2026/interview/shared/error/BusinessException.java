package com.agent2026.interview.shared.error;

import java.util.Objects;

/**
 * Exception for expected business failures whose code and message are safe to expose.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(normalizeMessage(errorCode, message));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(normalizeMessage(errorCode, message), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getCode() {
        return errorCode.getCode();
    }

    private static String normalizeMessage(ErrorCode errorCode, String message) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        return message == null || message.isBlank() ? errorCode.getMessage() : message;
    }
}

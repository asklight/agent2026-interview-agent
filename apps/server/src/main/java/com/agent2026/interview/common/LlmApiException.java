package com.agent2026.interview.common;

public class LlmApiException extends RuntimeException {

    private final int code;

    public LlmApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public LlmApiException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

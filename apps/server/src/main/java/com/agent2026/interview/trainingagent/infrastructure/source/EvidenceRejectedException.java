package com.agent2026.interview.trainingagent.infrastructure.source;

public class EvidenceRejectedException extends RuntimeException {
    private final String errorCode;

    public EvidenceRejectedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}

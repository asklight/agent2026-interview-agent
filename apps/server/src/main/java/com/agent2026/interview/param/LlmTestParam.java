package com.agent2026.interview.param;

import jakarta.validation.constraints.NotBlank;

public class LlmTestParam {

    @NotBlank(message = "message cannot be blank")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

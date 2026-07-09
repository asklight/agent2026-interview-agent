package com.agent2026.interview.vo;

public class LlmTestVO {

    private String model;
    private String content;
    private Integer totalTokens;
    private String responseTime;

    public LlmTestVO() {
    }

    public LlmTestVO(String model, String content, Integer totalTokens, String responseTime) {
        this.model = model;
        this.content = content;
        this.totalTokens = totalTokens;
        this.responseTime = responseTime;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public String getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(String responseTime) {
        this.responseTime = responseTime;
    }
}

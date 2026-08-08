package com.agent2026.interview.shared.error;

/**
 * Stable business error codes exposed by the HTTP API.
 */
public enum ErrorCode {

    PARAM_INVALID(40000, "请求参数不合法"),
    AUTH_REQUIRED(40100, "请先登录"),
    AUTH_CREDENTIALS_INVALID(40101, "用户名或密码错误"),
    AUTH_ACCESS_TOKEN_INVALID(40102, "登录凭证无效或已过期"),
    AUTH_REFRESH_TOKEN_INVALID(40103, "登录状态已失效，请重新登录"),
    AUTH_REFRESH_TOKEN_REPLAYED(40104, "检测到异常登录状态，请重新登录"),
    USERNAME_ALREADY_EXISTS(40930, "用户名已被使用"),
    USER_DISABLED(40310, "账号已被禁用"),
    RESOURCE_NOT_FOUND(40400, "请求的资源不存在"),
    PROJECT_PROFILE_NOT_FOUND(40401, "项目档案不存在"),
    PROJECT_PROFILE_ANALYSIS_FAILED(50001, "项目档案分析失败，请稍后重试"),
    PROJECT_PROFILE_NOT_READY(40901, "项目档案尚未准备完成"),
    PROJECT_PROFILE_ACCESS_DENIED(40301, "无权访问该项目档案"),
    PROJECT_PROFILE_STATE_CONFLICT(40902, "项目档案当前状态不允许执行该操作"),
    PROJECT_PROFILE_VERSION_CONFLICT(40903, "项目档案已被更新，请刷新后重试"),
    INTERVIEW_SESSION_NOT_FOUND(40402, "面试会话不存在"),
    INTERVIEW_SESSION_FINISHED(40910, "面试会话已经结束"),
    INTERVIEW_SESSION_ACCESS_DENIED(40302, "无权访问该面试会话"),
    INTERVIEW_STATE_CONFLICT(40911, "面试状态已变化，请刷新后重试"),
    INTERVIEW_DUPLICATE_TURN(40912, "该回答已提交"),
    INTERVIEW_TURN_PROCESSING(40913, "该回答正在处理中，请稍后重试"),
    ALGORITHM_PROBLEM_NOT_FOUND(40410, "算法题不存在"),
    ALGORITHM_SESSION_NOT_FOUND(40411, "算法训练不存在"),
    ALGORITHM_SESSION_ACCESS_DENIED(40311, "无权访问该算法训练"),
    ALGORITHM_SESSION_FINISHED(40940, "算法训练已经结束"),
    ALGORITHM_STATE_CONFLICT(40941, "算法训练状态已变化，请刷新后重试"),
    ALGORITHM_TURN_PROCESSING(40942, "该回答正在处理中，请稍后重试"),
    SIMULATION_NOT_FOUND(40420, "综合模拟面试不存在"),
    SIMULATION_ACCESS_DENIED(40320, "无权访问该综合模拟面试"),
    SIMULATION_STATE_CONFLICT(40950, "综合模拟状态已变化，请刷新后重试"),
    REPORT_NOT_READY(40920, "面试报告尚未生成"),
    RATE_LIMITED(42900, "请求过于频繁，请稍后重试"),
    LLM_RESPONSE_INVALID(50201, "大模型返回内容无法解析，请稍后重试"),
    LLM_UNAVAILABLE(50301, "大模型服务暂时不可用，请稍后重试"),
    INTERNAL_ERROR(50000, "服务器异常，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

package com.agent2026.interview.service;

import com.agent2026.interview.vo.InterviewReportVO;

public interface InterviewReportService {
    InterviewReportVO get(Long sessionId);
    void generateIfAbsent(Long sessionId);
}

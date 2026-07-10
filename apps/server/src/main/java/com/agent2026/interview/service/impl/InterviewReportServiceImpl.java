package com.agent2026.interview.service.impl;

import com.agent2026.interview.entity.InterviewAnswer;
import com.agent2026.interview.entity.InterviewReport;
import com.agent2026.interview.mapper.InterviewAnswerMapper;
import com.agent2026.interview.mapper.InterviewReportMapper;
import com.agent2026.interview.mapper.InterviewSessionMapper;
import com.agent2026.interview.service.InterviewReportService;
import com.agent2026.interview.vo.InterviewReportVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class InterviewReportServiceImpl implements InterviewReportService {
    private final InterviewReportMapper reportMapper;
    private final InterviewAnswerMapper answerMapper;
    private final InterviewSessionMapper sessionMapper;

    public InterviewReportServiceImpl(InterviewReportMapper reportMapper, InterviewAnswerMapper answerMapper,
                                      InterviewSessionMapper sessionMapper) {
        this.reportMapper = reportMapper;
        this.answerMapper = answerMapper;
        this.sessionMapper = sessionMapper;
    }

    @Override
    public InterviewReportVO get(Long sessionId) {
        if (sessionMapper.selectById(sessionId) == null) {
            throw new IllegalStateException("Interview session not found: " + sessionId);
        }
        InterviewReport report = reportMapper.selectOne(new LambdaQueryWrapper<InterviewReport>()
                .eq(InterviewReport::getSessionId, sessionId));
        if (report == null) {
            throw new IllegalStateException("Interview report is not available until the session is finished");
        }
        return toVO(report);
    }

    @Override
    public void generateIfAbsent(Long sessionId) {
        if (reportMapper.selectCount(new LambdaQueryWrapper<InterviewReport>().eq(InterviewReport::getSessionId, sessionId)) > 0) {
            return;
        }
        List<InterviewAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<InterviewAnswer>()
                .eq(InterviewAnswer::getSessionId, sessionId).orderByAsc(InterviewAnswer::getTurnIndex));
        List<InterviewAnswer> scored = answers.stream().filter(answer -> answer.getScore() != null).toList();
        BigDecimal score = scored.isEmpty() ? BigDecimal.ZERO : scored.stream().map(InterviewAnswer::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(scored.size()), 1, RoundingMode.HALF_UP);

        InterviewReport report = new InterviewReport();
        report.setSessionId(sessionId);
        report.setTotalScore(score);
        report.setScoreLevel(scoreLevel(score, scored.isEmpty()));
        report.setAnsweredCount(answers.size());
        report.setStrengths(join(strengths(answers, score, scored.isEmpty())));
        report.setWeaknesses(join(weaknesses(answers, score, scored.isEmpty())));
        report.setRecommendations(join(recommendations(score, scored.isEmpty())));
        reportMapper.insert(report);
    }

    private List<String> strengths(List<InterviewAnswer> answers, BigDecimal score, boolean noScore) {
        List<String> items = new ArrayList<>();
        if (!answers.isEmpty()) items.add("完成了 " + answers.size() + " 轮回答，训练记录已完整保存。");
        if (!noScore && score.compareTo(BigDecimal.valueOf(80)) >= 0) items.add("整体回答质量较好，核心知识表达具备面试竞争力。");
        if (items.isEmpty()) items.add("已建立训练会话，可继续提交回答获得个性化反馈。");
        return items;
    }

    private List<String> weaknesses(List<InterviewAnswer> answers, BigDecimal score, boolean noScore) {
        if (answers.isEmpty()) return List.of("本次训练没有提交有效回答，暂时无法评估知识薄弱点。");
        if (noScore) return List.of("学校 LLM 尚未配置，当前没有可量化的 AI 评分。", "建议配置后重新完成一轮训练以获得结构化点评。");
        if (score.compareTo(BigDecimal.valueOf(60)) < 0) return List.of("回答的关键知识点覆盖不足，需要先补齐基础概念和执行流程。", "建议回答时先给结论，再按原理、场景和边界展开。");
        if (score.compareTo(BigDecimal.valueOf(80)) < 0) return List.of("核心概念基本具备，但工程场景、边界条件或表达完整性仍有提升空间。");
        return List.of("可继续加强复杂故障排查和高并发场景下的取舍表达。");
    }

    private List<String> recommendations(BigDecimal score, boolean noScore) {
        if (noScore) return List.of("配置学校 tju-llm 后重跑训练，获得逐题命中要点和缺失点。", "每次回答保持“结论—原理—场景—边界”的结构。");
        if (score.compareTo(BigDecimal.valueOf(60)) < 0) return List.of("优先复习本模块题卡的评分点，再进行同难度训练。", "每题至少准备一个真实工程场景作为补充说明。");
        return List.of("切换到更高难度继续训练，并主动练习追问场景。", "将本次薄弱表达整理成自己的面试回答模板。");
    }

    private String scoreLevel(BigDecimal score, boolean noScore) {
        if (noScore) return "待评分";
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) return "优秀";
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) return "合格";
        return "待加强";
    }

    private String join(List<String> items) { return String.join("\n", items); }
    private List<String> split(String value) { return value == null || value.isBlank() ? List.of() : List.of(value.split("\\n")); }

    private InterviewReportVO toVO(InterviewReport report) {
        InterviewReportVO vo = new InterviewReportVO();
        vo.setSessionId(report.getSessionId());
        vo.setTotalScore(report.getTotalScore());
        vo.setScoreLevel(report.getScoreLevel());
        vo.setAnsweredCount(report.getAnsweredCount());
        vo.setStrengths(split(report.getStrengths()));
        vo.setWeaknesses(split(report.getWeaknesses()));
        vo.setRecommendations(split(report.getRecommendations()));
        vo.setGeneratedAt(report.getCreateTime());
        return vo;
    }
}

package com.agent2026.interview.interviewsimulation.application;

import com.agent2026.interview.interviewsimulation.persistence.SimulationAnswerReceiptMapper;
import com.agent2026.interview.param.SubmitAnswerParam;
import com.agent2026.interview.service.InterviewSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimulationKnowledgeSubmissionService {
    private final SimulationAnswerReceiptMapper receipts;
    private final InterviewSessionService knowledge;

    public SimulationKnowledgeSubmissionService(SimulationAnswerReceiptMapper receipts,
                                                InterviewSessionService knowledge) {
        this.receipts = receipts;
        this.knowledge = knowledge;
    }

    @Transactional
    public void submit(Long simulationId, Long sessionId, Long userId, String clientTurnId, String content) {
        if (receipts.claim(simulationId, clientTurnId, "KNOWLEDGE") == 0) {
            return;
        }
        SubmitAnswerParam answer = new SubmitAnswerParam();
        answer.setAnswerText(content.trim());
        var result = knowledge.submitAnswer(sessionId, answer, userId);
        if ("NEXT_QUESTION".equals(result.getNextAction())) {
            knowledge.nextQuestion(sessionId, userId);
        }
    }
}

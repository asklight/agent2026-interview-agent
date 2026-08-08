package com.agent2026.interview.interviewsimulation.application;

import com.agent2026.interview.interviewsimulation.persistence.SimulationAnswerReceiptMapper;
import com.agent2026.interview.service.InterviewSessionService;
import com.agent2026.interview.vo.SubmitAnswerVO;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimulationKnowledgeSubmissionServiceTest {
    private final SimulationAnswerReceiptMapper receipts = mock(SimulationAnswerReceiptMapper.class);
    private final InterviewSessionService knowledge = mock(InterviewSessionService.class);
    private final SimulationKnowledgeSubmissionService service =
            new SimulationKnowledgeSubmissionService(receipts, knowledge);

    @Test
    void duplicateClientTurnDoesNotSubmitTheKnowledgeAnswerAgain() {
        when(receipts.claim(5L, "turn-1", "KNOWLEDGE")).thenReturn(0);

        service.submit(5L, 12L, 7L, "turn-1", "answer");

        verify(knowledge, never()).submitAnswer(any(), any(), any());
    }

    @Test
    void movesToTheNextQuestionAfterAClaimedAnswer() {
        when(receipts.claim(5L, "turn-1", "KNOWLEDGE")).thenReturn(1);
        SubmitAnswerVO result = new SubmitAnswerVO();
        result.setNextAction("NEXT_QUESTION");
        when(knowledge.submitAnswer(any(), any(), any())).thenReturn(result);

        service.submit(5L, 12L, 7L, "turn-1", "answer");

        verify(knowledge).nextQuestion(12L, 7L);
    }
}

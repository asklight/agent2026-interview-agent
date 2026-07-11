package com.agent2026.interview.projectdeepdive.interview.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ProjectInterviewSessionResponseTest {
    @Test void publicResponseNeverContainsPrivateEvaluationFields() throws Exception {
        PublicInterviewTurnResponse turn = new PublicInterviewTurnResponse(1L, 1, "INTERVIEWER", "OPENING",
                "请介绍项目", "TEXT", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(new ProjectInterviewSessionResponse(
                9L, "PROJECT_DEEP_DIVE", "IN_PROGRESS", "PROJECT_OVERVIEW", "OWNERSHIP",
                0, 4, 3, "TEXT", List.of(turn)));
        assertThat(json).doesNotContain("score", "hitPoints", "missingPoints", "weaknesses", "decision",
                "modelRawResponse", "retrievalTrace", "processingStatus", "clientTurnId");
    }
}

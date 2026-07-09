package com.agent2026.interview.followup;

import com.agent2026.interview.entity.QuestionCard;
import com.agent2026.interview.evaluation.AnswerEvaluationResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

@Component
public class FollowUpDecider {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public FollowUpDecider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FollowUpDecision decideAfterMainAnswer(QuestionCard question, AnswerEvaluationResult evaluation) {
        if (evaluation != null
                && NextAction.ASK_FOLLOW_UP.equals(evaluation.getNextAction())
                && StringUtils.hasText(evaluation.getFollowUpQuestion())) {
            return new FollowUpDecision(NextAction.ASK_FOLLOW_UP, evaluation.getFollowUpQuestion());
        }

        if (evaluation != null && NextAction.NEXT_QUESTION.equals(evaluation.getNextAction())) {
            return new FollowUpDecision(NextAction.NEXT_QUESTION, null);
        }

        String followUp = firstQuestion(question.getFollowups());
        if (!StringUtils.hasText(followUp)) {
            followUp = firstQuestion(question.getScenarioFollowups());
        }
        if (!StringUtils.hasText(followUp)) {
            return new FollowUpDecision(NextAction.NEXT_QUESTION, null);
        }
        return new FollowUpDecision(NextAction.ASK_FOLLOW_UP, followUp);
    }

    private String firstQuestion(String jsonText) {
        if (!StringUtils.hasText(jsonText)) {
            return null;
        }
        try {
            List<String> values = objectMapper.readValue(jsonText, STRING_LIST);
            return values.stream()
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(null);
        } catch (IOException ex) {
            return null;
        }
    }
}

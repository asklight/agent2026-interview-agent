package com.agent2026.interview.evaluation;

import com.agent2026.interview.client.TjuLlmClient;
import com.agent2026.interview.entity.QuestionCard;
import com.agent2026.interview.followup.NextAction;
import com.agent2026.interview.vo.LlmTestVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AnswerEvaluator {

    private final TjuLlmClient tjuLlmClient;
    private final ObjectMapper objectMapper;

    public AnswerEvaluator(TjuLlmClient tjuLlmClient, ObjectMapper objectMapper) {
        this.tjuLlmClient = tjuLlmClient;
        this.objectMapper = objectMapper;
    }

    public AnswerEvaluationResult evaluate(QuestionCard question, String questionText, String answerText, String questionType) {
        if (!tjuLlmClient.isConfigured()) {
            return buildConfigRequiredEvaluation(question);
        }

        LlmTestVO response = tjuLlmClient.chat(buildEvaluationPrompt(question, questionText, answerText, questionType));
        return parseModelEvaluation(response.getContent(), questionType);
    }

    private AnswerEvaluationResult buildConfigRequiredEvaluation(QuestionCard question) {
        String evaluationText = """
                本地尚未配置学校 tju-llm API，本轮回答已保存，但暂不能生成 AI 点评。

                正式运行时请在后端环境变量中配置 TJU_LLM_API_URL 和 TJU_LLM_API_KEY。
                API Key 只允许放在后端环境变量中，不能提交到仓库，也不能放到前端。

                当前题卡参考要点：
                %s

                可用追问：
                %s
                """.formatted(question.getKeyPoints(), question.getFollowups());
        return AnswerEvaluationResult.unstructured(evaluationText, false);
    }

    private AnswerEvaluationResult parseModelEvaluation(String content, String questionType) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(content));
            Integer score = normalizedScore(root.path("score"));
            String followUpQuestion = normalizedText(root.path("followUpQuestion").asText(null));
            List<String> hitPoints = parseStringList(root.path("hitPoints"));
            List<String> missingPoints = parseStringList(root.path("missingPoints"));
            List<String> weaknesses = parseStringList(root.path("weaknesses"));
            String nextAction = inferNextAction(
                    normalizedNextAction(root.path("nextAction").asText(null)),
                    questionType,
                    score,
                    missingPoints,
                    weaknesses
            );
            if (NextAction.ASK_FOLLOW_UP.equals(nextAction) && !StringUtils.hasText(followUpQuestion)) {
                followUpQuestion = buildFallbackFollowUp(missingPoints, weaknesses);
            }
            String evaluationText = formatEvaluationText(
                    root.path("summary").asText(""),
                    root.path("accuracy").asText(""),
                    score,
                    hitPoints,
                    missingPoints,
                    weaknesses,
                    root.path("suggestion").asText(""),
                    nextAction,
                    followUpQuestion
            );
            if (!StringUtils.hasText(evaluationText)) {
                evaluationText = content;
            }
            return new AnswerEvaluationResult(
                    evaluationText,
                    content,
                    score,
                    hitPoints,
                    missingPoints,
                    weaknesses,
                    nextAction,
                    followUpQuestion,
                    true,
                    true
            );
        } catch (IOException | IllegalArgumentException ex) {
            return AnswerEvaluationResult.unstructured(content, true);
        }
    }

    private String extractJson(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("empty model response");
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("model response does not contain json object");
        }
        return trimmed.substring(start, end + 1);
    }

    private Integer normalizedScore(JsonNode scoreNode) {
        if (!scoreNode.isNumber()) {
            return null;
        }
        int score = scoreNode.asInt();
        if (score < 0) {
            return 0;
        }
        return Math.min(score, 100);
    }

    private String normalizedNextAction(String nextAction) {
        if (NextAction.ASK_FOLLOW_UP.equals(nextAction)) {
            return NextAction.ASK_FOLLOW_UP;
        }
        if (NextAction.NEXT_QUESTION.equals(nextAction)) {
            return NextAction.NEXT_QUESTION;
        }
        return null;
    }

    private String normalizedText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String inferNextAction(String modelAction,
                                   String questionType,
                                   Integer score,
                                   List<String> missingPoints,
                                   List<String> weaknesses) {
        if ("FOLLOW_UP".equals(questionType)) {
            return NextAction.NEXT_QUESTION;
        }
        if (NextAction.ASK_FOLLOW_UP.equals(modelAction) || NextAction.NEXT_QUESTION.equals(modelAction)) {
            return modelAction;
        }
        if (score != null && score < 75) {
            return NextAction.ASK_FOLLOW_UP;
        }
        if ((missingPoints != null && !missingPoints.isEmpty()) || (weaknesses != null && !weaknesses.isEmpty())) {
            return NextAction.ASK_FOLLOW_UP;
        }
        return NextAction.NEXT_QUESTION;
    }

    private String buildFallbackFollowUp(List<String> missingPoints, List<String> weaknesses) {
        if (missingPoints != null && !missingPoints.isEmpty()) {
            return "你刚才没有讲清楚“" + missingPoints.get(0) + "”，请结合实际项目或底层原理再说明一下。";
        }
        if (weaknesses != null && !weaknesses.isEmpty()) {
            return "你刚才的回答存在“" + weaknesses.get(0) + "”的问题，请重新组织一下，并补充一个具体例子。";
        }
        return "请你结合一个真实项目场景，进一步说明这个问题的关键点。";
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String value = item.asText("");
                if (StringUtils.hasText(value)) {
                    values.add(value.trim());
                }
            }
            return values;
        }
        if (node.isTextual()) {
            String[] parts = node.asText("").split("\\r?\\n|；|;");
            for (String item : parts) {
                String value = item.replaceFirst("^[-*\\d.、\\s]+", "").trim();
                if (StringUtils.hasText(value)) {
                    values.add(value);
                }
            }
            return values;
        }
        if (!node.isMissingNode() && !node.isNull()) {
            String value = node.asText("");
            if (StringUtils.hasText(value)) {
                values.add(value.trim());
            }
        }
        return values;
    }

    private String formatEvaluationText(String summary,
                                        String accuracy,
                                        Integer score,
                                        List<String> hitPoints,
                                        List<String> missingPoints,
                                        List<String> weaknesses,
                                        String suggestion,
                                        String nextAction,
                                        String followUpQuestion) {
        StringBuilder builder = new StringBuilder();
        appendSection(builder, "总体评价", summary);
        appendSection(builder, "准确性判断", accuracy);
        if (score != null) {
            appendSection(builder, "建议分数", score + "/100");
        }
        appendListSection(builder, "命中要点", hitPoints);
        appendListSection(builder, "遗漏要点", missingPoints);
        appendListSection(builder, "薄弱表现", weaknesses);
        appendSection(builder, "改进建议", suggestion);
        if (StringUtils.hasText(nextAction)) {
            appendSection(builder, "下一步动作", nextAction);
        }
        if (StringUtils.hasText(followUpQuestion)) {
            appendSection(builder, "建议追问", followUpQuestion);
        }
        return builder.toString().trim();
    }

    private void appendSection(StringBuilder builder, String title, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append("【").append(title).append("】\n")
                .append(value.trim())
                .append("\n\n");
    }

    private void appendListSection(StringBuilder builder, String title, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        builder.append("【").append(title).append("】\n");
        for (String value : values) {
            builder.append("- ").append(value).append('\n');
        }
        builder.append('\n');
    }

    private String buildEvaluationPrompt(QuestionCard question, String questionText, String answerText, String questionType) {
        return """
                你是 Java 后端技术面试官。请根据题卡评分点，评价候选人对当前问题的回答，并决定下一步面试动作。

                只允许输出一个 JSON 对象，不要输出 Markdown，不要输出解释性前后缀。

                JSON 字段要求：
                {
                  "score": 0-100 的整数,
                  "summary": "一句话总评",
                  "accuracy": "回答是否准确，以及主要原因",
                  "hitPoints": ["候选人命中的关键点"],
                  "missingPoints": ["候选人遗漏的关键点"],
                  "weaknesses": ["理解或表达上的薄弱点"],
                  "suggestion": "下一步改进建议",
                  "nextAction": "ASK_FOLLOW_UP 或 NEXT_QUESTION",
                  "followUpQuestion": "当 nextAction 为 ASK_FOLLOW_UP 时给出一个具体追问，否则为空字符串"
                }

                动作决策规则：
                1. 当前问题类型为 FOLLOW_UP 时，nextAction 必须为 NEXT_QUESTION。
                2. 当前问题类型为 MAIN，且候选人遗漏关键点、表达过浅或没有结合工程场景时，nextAction 优先为 ASK_FOLLOW_UP。
                3. 追问必须针对候选人的回答缺口生成，不要简单照抄题卡追问。
                4. 如果回答已经较完整，nextAction 为 NEXT_QUESTION。

                【当前问题类型】
                %s

                【当前问题】
                %s

                【题卡主问题】
                %s

                【标准要点】
                %s

                【常见错误】
                %s

                【题卡备用追问】
                %s

                【场景追问】
                %s

                【评分标准】
                %s

                【候选人回答】
                %s
                """.formatted(
                questionType,
                questionText,
                question.getMainQuestion(),
                question.getKeyPoints(),
                question.getCommonMistakes(),
                question.getFollowups(),
                question.getScenarioFollowups(),
                question.getScoringRubric(),
                answerText
        );
    }
}

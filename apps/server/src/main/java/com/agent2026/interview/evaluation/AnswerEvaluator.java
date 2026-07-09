package com.agent2026.interview.evaluation;

import com.agent2026.interview.client.TjuLlmClient;
import com.agent2026.interview.entity.QuestionCard;
import com.agent2026.interview.vo.LlmTestVO;
import org.springframework.stereotype.Component;

@Component
public class AnswerEvaluator {

    private final TjuLlmClient tjuLlmClient;

    public AnswerEvaluator(TjuLlmClient tjuLlmClient) {
        this.tjuLlmClient = tjuLlmClient;
    }

    public AnswerEvaluationResult evaluate(QuestionCard question, String questionText, String answerText, String questionType) {
        if (!tjuLlmClient.isConfigured()) {
            return new AnswerEvaluationResult(buildConfigRequiredEvaluation(question), false);
        }

        LlmTestVO response = tjuLlmClient.chat(buildEvaluationPrompt(question, questionText, answerText, questionType));
        return new AnswerEvaluationResult(response.getContent(), true);
    }

    private String buildConfigRequiredEvaluation(QuestionCard question) {
        return """
                本地尚未配置学校 tju-llm API，本轮回答已保存，但暂不能生成 AI 点评。

                正式运行时请在后端环境变量中配置 TJU_LLM_API_URL 和 TJU_LLM_API_KEY。
                API Key 只允许放在后端环境变量中，不能提交到仓库，也不能放到前端。

                当前题卡参考要点：
                %s

                可用追问：
                %s
                """.formatted(question.getKeyPoints(), question.getFollowups());
    }

    private String buildEvaluationPrompt(QuestionCard question, String questionText, String answerText, String questionType) {
        return """
                你是 Java 后端技术面试官。请根据题卡评分点，评价候选人对当前问题的回答。

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

                【可选追问】
                %s

                【场景追问】
                %s

                【评分标准】
                %s

                【候选人回答】
                %s

                请用中文输出：
                1. 回答是否准确
                2. 命中的关键点
                3. 漏掉的关键点
                4. 表达或理解上的风险
                5. 如果还需要追问，给出一个最值得追问的问题
                6. 0-100 的建议分数
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

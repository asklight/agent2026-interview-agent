package com.agent2026.interview.engine;

import com.agent2026.interview.entity.QuestionCard;
import com.agent2026.interview.mapper.QuestionCardMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;

@Component
public class QuestionSelector {

    private final QuestionCardMapper questionCardMapper;

    public QuestionSelector(QuestionCardMapper questionCardMapper) {
        this.questionCardMapper = questionCardMapper;
    }

    public QuestionCard selectFirst(String module, String difficulty) {
        QuestionCard question = selectNext(module, difficulty, null);
        if (question == null) {
            throw new IllegalStateException("No enabled question card found for module=" + module);
        }
        return question;
    }

    public QuestionCard selectNext(String module, String difficulty, Collection<Long> excludedIds) {
        LambdaQueryWrapper<QuestionCard> query = new LambdaQueryWrapper<QuestionCard>()
                .eq(QuestionCard::getEnabled, 1)
                .eq(QuestionCard::getModule, module)
                .orderByAsc(QuestionCard::getId)
                .last("LIMIT 1");
        if (StringUtils.hasText(difficulty)) {
            query.eq(QuestionCard::getDifficulty, difficulty);
        }
        if (excludedIds != null && !excludedIds.isEmpty()) {
            query.notIn(QuestionCard::getId, excludedIds);
        }
        return questionCardMapper.selectOne(query);
    }
}

package com.agent2026.interview.service.impl;

import com.agent2026.interview.entity.QuestionCard;
import com.agent2026.interview.mapper.QuestionCardMapper;
import com.agent2026.interview.service.QuestionCardService;
import com.agent2026.interview.vo.QuestionCardVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class QuestionCardServiceImpl implements QuestionCardService {

    private final QuestionCardMapper questionCardMapper;

    public QuestionCardServiceImpl(QuestionCardMapper questionCardMapper) {
        this.questionCardMapper = questionCardMapper;
    }

    @Override
    public List<String> listModules() {
        return questionCardMapper.selectEnabledModules();
    }

    @Override
    public List<QuestionCardVO> listCards(String module, String difficulty) {
        LambdaQueryWrapper<QuestionCard> query = new LambdaQueryWrapper<QuestionCard>()
                .eq(QuestionCard::getEnabled, 1)
                .orderByAsc(QuestionCard::getModule)
                .orderByAsc(QuestionCard::getDifficulty)
                .orderByAsc(QuestionCard::getId);
        if (StringUtils.hasText(module)) {
            query.eq(QuestionCard::getModule, module);
        }
        if (StringUtils.hasText(difficulty)) {
            query.eq(QuestionCard::getDifficulty, difficulty);
        }
        return questionCardMapper.selectList(query).stream()
                .map(this::toVO)
                .toList();
    }

    private QuestionCardVO toVO(QuestionCard card) {
        QuestionCardVO vo = new QuestionCardVO();
        vo.setId(card.getId());
        vo.setCardCode(card.getCardCode());
        vo.setModule(card.getModule());
        vo.setDifficulty(card.getDifficulty());
        vo.setMainQuestion(card.getMainQuestion());
        vo.setKeyPoints(card.getKeyPoints());
        vo.setCommonMistakes(card.getCommonMistakes());
        vo.setFollowups(card.getFollowups());
        vo.setScenarioFollowups(card.getScenarioFollowups());
        vo.setScoringRubric(card.getScoringRubric());
        vo.setTags(card.getTags());
        return vo;
    }
}

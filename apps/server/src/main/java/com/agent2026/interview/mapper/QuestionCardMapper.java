package com.agent2026.interview.mapper;

import com.agent2026.interview.entity.QuestionCard;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionCardMapper extends BaseMapper<QuestionCard> {

    @Select("SELECT DISTINCT module FROM question_card WHERE enabled = 1 ORDER BY module")
    List<String> selectEnabledModules();
}

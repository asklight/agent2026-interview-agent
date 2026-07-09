package com.agent2026.interview.mapper;

import com.agent2026.interview.entity.InterviewAnswer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InterviewAnswerMapper extends BaseMapper<InterviewAnswer> {

    @Select("SELECT COUNT(*) FROM interview_answer WHERE session_id = #{sessionId}")
    int countBySessionId(Long sessionId);
}

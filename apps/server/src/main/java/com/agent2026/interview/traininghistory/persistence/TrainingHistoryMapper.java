package com.agent2026.interview.traininghistory.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TrainingHistoryMapper extends BaseMapper<TrainingHistoryEntity> {
    @Insert("""
            INSERT INTO training_history(user_id, training_type, source_session_id, status, title, summary,
                                         started_at, finished_at)
            SELECT s.user_id,
                   CASE WHEN s.mode='PROJECT_DEEP_DIVE' THEN 'PROJECT_DEEP_DIVE' ELSE 'KNOWLEDGE' END,
                   s.id, s.status,
                   CASE WHEN s.mode='PROJECT_DEEP_DIVE' THEN COALESCE(p.project_name, '项目深挖')
                        ELSE CONCAT(COALESCE(s.module, '综合'), '八股练习') END,
                   CASE WHEN s.mode='PROJECT_DEEP_DIVE' THEN p.summary
                        ELSE CONCAT('难度：', COALESCE(s.difficulty, '混合'), '，完成 ', s.completed_question_count,
                                    '/', s.question_count, ' 题') END,
                   s.start_time, s.end_time
            FROM interview_session s
            LEFT JOIN project_profile p ON p.id=s.project_profile_id
            WHERE s.user_id=#{userId} AND s.simulation_id IS NULL
            ON DUPLICATE KEY UPDATE status=VALUES(status), title=VALUES(title), summary=VALUES(summary),
                                    finished_at=VALUES(finished_at), user_id=VALUES(user_id)
            """)
    int syncInterviews(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO training_history(user_id, training_type, source_session_id, status, title, summary,
                                         started_at, finished_at)
            SELECT s.user_id, 'ALGORITHM', s.id, s.status, p.title,
                   CONCAT(CASE p.difficulty WHEN 'easy' THEN '简单' WHEN 'medium' THEN '中等' ELSE '困难' END,
                          ' · ', REPLACE(p.tags, ',', ' / ')),
                   s.started_at, s.finished_at
            FROM algorithm_session s JOIN algorithm_problem p ON p.id=s.problem_id
            WHERE s.user_id=#{userId} AND s.simulation_id IS NULL
            ON DUPLICATE KEY UPDATE status=VALUES(status), title=VALUES(title), summary=VALUES(summary),
                                    finished_at=VALUES(finished_at), user_id=VALUES(user_id)
            """)
    int syncAlgorithms(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO training_history(user_id, training_type, source_session_id, status, title, summary,
                                         started_at, finished_at)
            SELECT user_id, 'COMPREHENSIVE_SIMULATION', id, status, 'Java 综合模拟面试',
                   '项目深挖 / 八股练习 / 算法口述', started_at, finished_at
            FROM simulation_session WHERE user_id=#{userId}
            ON DUPLICATE KEY UPDATE status=VALUES(status), finished_at=VALUES(finished_at), user_id=VALUES(user_id)
            """)
    int syncSimulations(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT * FROM training_history
            WHERE user_id=#{userId} AND hidden=0
            <if test="type != null and type != ''">AND training_type=#{type}</if>
            <if test="status != null and status != ''">AND status=#{status}</if>
            ORDER BY started_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<TrainingHistoryEntity> page(@Param("userId") Long userId, @Param("type") String type,
                                     @Param("status") String status, @Param("limit") int limit,
                                     @Param("offset") int offset);

    @Select("""
            <script>
            SELECT COUNT(*) FROM training_history
            WHERE user_id=#{userId} AND hidden=0
            <if test="type != null and type != ''">AND training_type=#{type}</if>
            <if test="status != null and status != ''">AND status=#{status}</if>
            </script>
            """)
    long countVisible(@Param("userId") Long userId, @Param("type") String type,
                      @Param("status") String status);

    @Update("UPDATE training_history SET hidden=1 WHERE id=#{id} AND user_id=#{userId} AND hidden=0")
    int hide(@Param("id") Long id, @Param("userId") Long userId);
}

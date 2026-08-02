package com.agent2026.interview.projectdeepdive.interview.persistence;

import com.agent2026.interview.entity.InterviewSession;
import com.agent2026.interview.mapper.InterviewSessionMapper;
import com.agent2026.interview.projectdeepdive.interview.domain.PlannedProbe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProjectInterviewRepositoryMySqlTest {

    @Autowired
    private ProjectInterviewRepository repository;
    @Autowired
    private InterviewSessionMapper sessionMapper;
    @Autowired
    private InterviewTurnMapper turnMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> sessionIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        sessionIds.forEach(sessionMapper::deleteById);
    }

    @Test
    void twoConcurrentAnswersForOneQuestionRegisterOnlyOneCandidate() throws Exception {
        Fixture fixture = createFixture();
        PlannedProbe probe = new PlannedProbe("mysql-concurrency", null, "OWNERSHIP", "ownership");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                return repository.registerCandidate(fixture.sessionId(), 0, "mysql-client-a",
                        fixture.questionTurnId(), "first answer", "TEXT", probe);
            });
            var second = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                return repository.registerCandidate(fixture.sessionId(), 0, "mysql-client-b",
                        fixture.questionTurnId(), "second answer", "TEXT", probe);
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var registrations = Arrays.asList(
                    first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(registrations).filteredOn(value -> value != null && value.processingOwner()).hasSize(1);
            assertThat(registrations).filteredOn(Objects::isNull).hasSize(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM interview_turn WHERE session_id=? AND role='CANDIDATE'",
                    Integer.class, fixture.sessionId())).isEqualTo(1);
            assertThat(sessionMapper.selectById(fixture.sessionId()).getVersion()).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void mysqlLeaseComparisonUsesThePersistedMillisecondToken() {
        Fixture fixture = createFixture();
        LocalDateTime lease = LocalDateTime.of(2026, 8, 2, 10, 0, 0, 123_000_000);
        InterviewTurnEntity candidate = candidate(fixture, lease);
        turnMapper.insert(candidate);

        assertThat(repository.findByClientTurnId(fixture.sessionId(), "mysql-lease").orElseThrow()
                .processingStartedAt()).isEqualTo(lease);
        assertThat(repository.markRetryable(candidate.getId(), lease.plusNanos(1_000_000))).isFalse();
        assertThat(repository.markRetryable(candidate.getId(), lease)).isTrue();
        assertThat(repository.claimRetry(candidate.getId(), LocalDateTime.now().minusMinutes(2))).isTrue();

        var reclaimed = repository.findByClientTurnId(fixture.sessionId(), "mysql-lease").orElseThrow();
        assertThat(reclaimed.processingStatus()).isEqualTo("PROCESSING");
        assertThat(reclaimed.processingStartedAt()).isNotEqualTo(lease);
        assertThat(reclaimed.processingStartedAt().getNano() % 1_000_000).isZero();
    }

    private Fixture createFixture() {
        LocalDateTime now = LocalDateTime.now();
        InterviewSession session = new InterviewSession();
        session.setMode("PROJECT_DEEP_DIVE");
        session.setFeedbackTiming("AFTER_SESSION");
        session.setQuestionCount(1);
        session.setCompletedQuestionCount(0);
        session.setStatus("IN_PROGRESS");
        session.setConversationPhase("PROJECT_OVERVIEW");
        session.setCurrentProbeDimension("OWNERSHIP");
        session.setFollowUpCount(0);
        session.setMaxFollowUpCount(3);
        session.setInputModality("TEXT");
        session.setVersion(0L);
        session.setStartTime(now);
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.insert(session);
        sessionIds.add(session.getId());

        InterviewTurnEntity opening = new InterviewTurnEntity();
        opening.setSessionId(session.getId());
        opening.setSequenceNo(1);
        opening.setRole("INTERVIEWER");
        opening.setTurnType("OPENING");
        opening.setContent("question");
        opening.setInputModality("TEXT");
        opening.setProbeId("mysql-concurrency");
        opening.setProbeDimension("OWNERSHIP");
        opening.setProcessingStatus("COMPLETED");
        opening.setStartedAt(now);
        opening.setEndedAt(now);
        opening.setCreateTime(now);
        turnMapper.insert(opening);
        return new Fixture(session.getId(), opening.getId());
    }

    private InterviewTurnEntity candidate(Fixture fixture, LocalDateTime lease) {
        InterviewTurnEntity candidate = new InterviewTurnEntity();
        candidate.setSessionId(fixture.sessionId());
        candidate.setSequenceNo(2);
        candidate.setRole("CANDIDATE");
        candidate.setTurnType("ANSWER");
        candidate.setContent("answer");
        candidate.setInputModality("TEXT");
        candidate.setParentTurnId(fixture.questionTurnId());
        candidate.setProbeId("mysql-lease-probe");
        candidate.setProbeDimension("OWNERSHIP");
        candidate.setProcessingStatus("PROCESSING");
        candidate.setProcessingStartedAt(lease);
        candidate.setClientTurnId("mysql-lease");
        candidate.setStartedAt(lease);
        candidate.setCreateTime(lease);
        return candidate;
    }

    private record Fixture(Long sessionId, Long questionTurnId) {
    }
}

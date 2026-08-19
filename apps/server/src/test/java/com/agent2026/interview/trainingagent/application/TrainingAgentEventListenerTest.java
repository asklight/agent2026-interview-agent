package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.shared.training.TrainingCompletedEvent;
import com.agent2026.interview.shared.training.CompletedTrainingReportRef;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TrainingAgentEventListenerTest {

    @Test
    void delegatesCommittedTrainingEventToEvidenceSynchronization() throws Exception {
        EvidenceSynchronizationService synchronization = mock(EvidenceSynchronizationService.class);
        TrainingAgentEventListener listener = new TrainingAgentEventListener(synchronization);
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 19, 9, 30);
        TrainingCompletedEvent event = new TrainingCompletedEvent(
                7L, "PROJECT_DEEP_DIVE", 41L, 1, completedAt);

        listener.onTrainingCompleted(event);

        verify(synchronization).registerAndSynchronize(
                new CompletedTrainingReportRef("PROJECT_DEEP_DIVE", 7L, 41L, 1, completedAt));

        Method method = TrainingAgentEventListener.class
                .getDeclaredMethod("onTrainingCompleted", TrainingCompletedEvent.class);
        TransactionalEventListener eventListener = method.getAnnotation(TransactionalEventListener.class);
        assertThat(eventListener).isNotNull();
        assertThat(eventListener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(eventListener.fallbackExecution()).isTrue();
        assertThat(method.getAnnotation(Async.class)).isNotNull();
    }
}

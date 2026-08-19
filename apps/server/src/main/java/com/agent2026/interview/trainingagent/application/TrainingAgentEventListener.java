package com.agent2026.interview.trainingagent.application;

import com.agent2026.interview.shared.training.TrainingCompletedEvent;
import com.agent2026.interview.shared.training.CompletedTrainingReportRef;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TrainingAgentEventListener {
    private final EvidenceSynchronizationService synchronization;

    public TrainingAgentEventListener(EvidenceSynchronizationService synchronization) {
        this.synchronization = synchronization;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTrainingCompleted(TrainingCompletedEvent event) {
        synchronization.registerAndSynchronize(new CompletedTrainingReportRef(event.sourceType(), event.userId(),
                event.sourceSessionId(), event.sourceReportVersion(), event.completedAt()));
    }
}

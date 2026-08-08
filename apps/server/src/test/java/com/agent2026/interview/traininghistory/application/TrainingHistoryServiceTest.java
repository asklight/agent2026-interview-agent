package com.agent2026.interview.traininghistory.application;

import com.agent2026.interview.shared.error.BusinessException;
import com.agent2026.interview.traininghistory.persistence.TrainingHistoryEntity;
import com.agent2026.interview.traininghistory.persistence.TrainingHistoryMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainingHistoryServiceTest {
    private final TrainingHistoryMapper mapper = mock(TrainingHistoryMapper.class);
    private final TrainingHistoryService service = new TrainingHistoryService(mapper);

    @Test
    void rebuildsBothSourcesBeforeReturningTheUsersPage() {
        TrainingHistoryEntity item = new TrainingHistoryEntity();
        item.setId(1L); item.setUserId(7L); item.setTrainingType("ALGORITHM"); item.setSourceSessionId(9L);
        item.setStatus("IN_PROGRESS"); item.setTitle("两数之和");
        when(mapper.page(7L, null, null, 20, 0)).thenReturn(List.of(item));
        when(mapper.countVisible(7L, null, null)).thenReturn(1L);

        var result = service.list(7L, null, null, 1, 20);

        var order = inOrder(mapper);
        order.verify(mapper).syncInterviews(7L);
        order.verify(mapper).syncAlgorithms(7L);
        order.verify(mapper).syncSimulations(7L);
        order.verify(mapper).page(7L, null, null, 20, 0);
        assertThat(result.items()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    void cannotHideAnotherUsersHistoryItem() {
        when(mapper.hide(3L, 7L)).thenReturn(0);
        assertThatThrownBy(() -> service.hide(7L, 3L)).isInstanceOf(BusinessException.class);
        verify(mapper).hide(3L, 7L);
    }
}

package com.agent2026.interview.projectdeepdive.interview.knowledge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "rag-toolkit", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVectorRetrievalService implements VectorRetrievalService {
    @Override public RetrievalContext retrieve(String query) { return new RetrievalContext(List.of(), false); }
}

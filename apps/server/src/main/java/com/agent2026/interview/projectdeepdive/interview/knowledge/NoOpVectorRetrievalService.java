package com.agent2026.interview.projectdeepdive.interview.knowledge;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NoOpVectorRetrievalService implements VectorRetrievalService {
    @Override public RetrievalContext retrieve(String query) { return new RetrievalContext(List.of(), false); }
}

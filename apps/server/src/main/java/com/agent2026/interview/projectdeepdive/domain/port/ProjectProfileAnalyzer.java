package com.agent2026.interview.projectdeepdive.domain.port;

import com.agent2026.interview.projectdeepdive.domain.model.ProjectProfileAnalysis;

public interface ProjectProfileAnalyzer {

    ProjectProfileAnalysis analyze(String sanitizedDescription);
}

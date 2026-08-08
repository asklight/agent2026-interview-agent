package com.agent2026.interview.interviewsimulation.api;

import com.agent2026.interview.algorithmpractice.api.AlgorithmProblemResponse;

import java.util.List;

public record SimulationOptionsResponse(List<ProjectOption> projects,
                                        List<AlgorithmProblemResponse> algorithmProblems,
                                        List<String> knowledgeModules,
                                        List<String> difficulties) {
    public SimulationOptionsResponse {
        projects = projects == null ? List.of() : List.copyOf(projects);
        algorithmProblems = algorithmProblems == null ? List.of() : List.copyOf(algorithmProblems);
        knowledgeModules = knowledgeModules == null ? List.of() : List.copyOf(knowledgeModules);
        difficulties = difficulties == null ? List.of() : List.copyOf(difficulties);
    }

    public record ProjectOption(Long id, String name, String summary, List<String> techStack) {
        public ProjectOption {
            techStack = techStack == null ? List.of() : List.copyOf(techStack);
        }
    }
}

package com.agent2026.interview.projectdeepdive.interview.knowledge;

import java.util.List;

public record RetrievalContext(List<String> snippets, boolean degraded) {
    public RetrievalContext { snippets = snippets == null ? List.of() : List.copyOf(snippets); }
}

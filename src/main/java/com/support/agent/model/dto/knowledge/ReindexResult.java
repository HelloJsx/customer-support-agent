package com.support.agent.model.dto.knowledge;

import java.time.Instant;

public record ReindexResult(int documents, int segments, Instant indexedAt) {
}

package com.unbumpkin.codechat.model.openai;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VectorStoreResponse(
    String id,
    String object,
    @JsonProperty("created_at") long createdAt,
    String name,
    @JsonProperty("usage_bytes") long usageBytes,
    @JsonProperty("file_counts") FileCounts fileCounts,
    String status,
    @JsonProperty("expires_after") ExpiresAfter expiresAfter,
    @JsonProperty("expires_at") long expiresAt,
    @JsonProperty("last_active_at") long lastActiveAt,
    Map<String, String> metadata
) {
    public record FileCounts(
        @JsonProperty("in_progress") int inProgress,
        int completed,
        int failed,
        int cancelled,
        int total
    ) {}

    public record ExpiresAfter(
        String anchor,
        int days
    ) {}
}
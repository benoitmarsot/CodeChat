package com.unbumpkin.codechat.model;

import java.sql.Timestamp;

import com.unbumpkin.codechat.dto.openai.AssistantTypes;

public record Discussion(
    int did,
    int projectId,
    String name,
    String description,
    boolean isFavorite,
    AssistantTypes assistantType,
    Timestamp created
) {
}
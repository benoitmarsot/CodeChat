package com.unbumpkin.codechat.dto.request;

import com.unbumpkin.codechat.dto.openai.AssistantTypes;

public record DiscussionUpdateRequest(
    int did,
    String name,
    String description,
    boolean isFavorite,
    AssistantTypes assistantType
) {}
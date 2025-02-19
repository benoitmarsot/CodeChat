package com.unbumpkin.codechat.domain.openai;

public record Thread(
    int threadid,
    String oaiThreadId,
    Integer vsid,
    int discussionId,
    String type
) {
}
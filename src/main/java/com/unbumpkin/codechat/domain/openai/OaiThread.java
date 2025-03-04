package com.unbumpkin.codechat.domain.openai;

public record OaiThread(
    int threadid,
    String oaiThreadId,
    Integer vsid,
    int did,
    String type
) {
}
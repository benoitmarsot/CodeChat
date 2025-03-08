package com.unbumpkin.codechat.model.openai;

public record OaiThread(
    int threadid,
    String oaiThreadId,
    Integer vsid,
    int did,
    String type
) {
}
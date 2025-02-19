package com.unbumpkin.codechat.domain;

public record Message(
    int msgid,
    int did,
    String role,
    int authorid,
    String message
) {
}
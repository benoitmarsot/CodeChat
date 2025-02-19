package com.unbumpkin.codechat.domain;

public record Message(
    int msgid,
    int discussionId,
    String role,
    int authorid,
    String message
) {
}
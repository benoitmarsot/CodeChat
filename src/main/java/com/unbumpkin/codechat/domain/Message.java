package com.unbumpkin.codechat.domain;

import java.sql.Timestamp;

public record Message(
    int msgid,
    int discussionId,
    String role,
    int authorid,
    String message,
    Timestamp created
) {
}
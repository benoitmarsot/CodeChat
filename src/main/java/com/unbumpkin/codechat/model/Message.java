package com.unbumpkin.codechat.model;

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
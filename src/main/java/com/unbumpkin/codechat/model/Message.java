package com.unbumpkin.codechat.model;

import java.sql.Timestamp;

public record Message(
    int msgid,
    int discussionId,
    String role,
    int authorid,
    String message,
    String socialAnswer,
    Timestamp created
) {
    public Message(int msgid, int discussionId, String role, int authorid, String message, Timestamp created) {
        this(msgid, discussionId, role, authorid, message, null, created);
    }

    public Message(Message message, String socialAnswer) {
        this(
            message.msgid,
            message.discussionId,
            message.role,
            message.authorid,
            message.message,
            socialAnswer,
            message.created
        );
    }
}
package com.unbumpkin.codechat.model;

import java.sql.Timestamp;

import com.unbumpkin.codechat.dto.social.SocialReferences;

public record Message(
    int msgid,
    int discussionId,
    String role,
    int authorid,
    String message,
    SocialReferences socialAnswer,
    Timestamp created
) {
    public Message(int msgid, int discussionId, String role, int authorid, String message, Timestamp created) {
        this(msgid, discussionId, role, authorid, message, null, created);
    }

    public Message(Message message, SocialReferences socialAnswer) {
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
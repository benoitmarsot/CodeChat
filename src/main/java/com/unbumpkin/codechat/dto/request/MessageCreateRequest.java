package com.unbumpkin.codechat.dto.request;

import com.unbumpkin.codechat.dto.social.SocialReferences;

public record MessageCreateRequest ( int did, String role, String message, SocialReferences socialAnswer ) {
    public MessageCreateRequest(int did, String role, String message) {
        this(did, role, message, null);
    }
}

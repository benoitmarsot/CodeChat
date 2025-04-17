package com.unbumpkin.codechat.dto.social;

public record SocialMessage(
    String userId,
    String ts,
    String message
) {}

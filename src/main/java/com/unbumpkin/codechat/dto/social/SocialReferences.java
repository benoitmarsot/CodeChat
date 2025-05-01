package com.unbumpkin.codechat.dto.social;

import java.util.List;

public record SocialReferences(
    String overallDescription,
    List<SocialReference> messages
) {}
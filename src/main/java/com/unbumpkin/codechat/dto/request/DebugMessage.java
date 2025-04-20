package com.unbumpkin.codechat.dto.request;

import java.sql.Timestamp;

public record DebugMessage(
   
    String message,
    Timestamp created,
    int level
) {
}

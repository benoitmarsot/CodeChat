package com.unbumpkin.codechat.model;

import java.sql.Timestamp;

public record DebugMessage(
   
    String message,
    Timestamp created,
    int level
) {
}

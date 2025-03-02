package com.unbumpkin.codechat.domain;

import java.sql.Timestamp;

public record Discussion(
    int did,
    int projectId,
    String name,
    String description,
    boolean isFavorite,
    Timestamp created
) {
}
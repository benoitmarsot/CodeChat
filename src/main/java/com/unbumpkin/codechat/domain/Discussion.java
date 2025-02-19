package com.unbumpkin.codechat.domain;

public record Discussion(
    int did,
    int projectId,
    String name,
    String description
) {
}
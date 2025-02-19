package com.unbumpkin.codechat.domain;

public record Project(
    int projectId, 
    String name, 
    String description, 
    int authorId, 
    int assistantId
) {
}
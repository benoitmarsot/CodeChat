package com.unbumpkin.codechat.model;

public record Project(
    int projectId, 
    String name, 
    String description, 
    int authorId, 
    int assistantId
) {
}
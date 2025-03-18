package com.unbumpkin.codechat.dto.request;

public record CreateProjectRequest(
    String name, String description, String sourcePath, String repoURL, String branch,
    String username, String password
) {
}

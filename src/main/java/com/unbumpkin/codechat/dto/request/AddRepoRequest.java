package com.unbumpkin.codechat.dto.request;

public record AddRepoRequest(
    int projectId, String sourcePath, String repoURL, String branch,
    String username, String password
) {
    
}

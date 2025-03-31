package com.unbumpkin.codechat.dto.request;

public record AddRepoRequest(
    int projectId, String repoURL, String branch,
    String username, String password
) {
    
}

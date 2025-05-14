package com.unbumpkin.codechat.dto.request;

public record AddRepoRequest(
    int projectId, String repoURL, String branch,
    String username, String password, boolean selfChunk
) {
    public AddRepoRequest(int projectId, String repoURL, String branch,
        String username, String password
    ) {
        this(projectId, repoURL, branch, username, password, false);
    }
    
}

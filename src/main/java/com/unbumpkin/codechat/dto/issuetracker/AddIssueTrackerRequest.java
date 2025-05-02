package com.unbumpkin.codechat.dto.issuetracker;

import com.unbumpkin.codechat.service.issuetracking.IssueTrackingService.IssueTrackingPlatforms;

public record AddIssueTrackerRequest(int projectId, String workspaceId, 
    IssueTrackingPlatforms platform, String jiraUrl, String userName, String password, String repositoryName, String pat, boolean selfChunk
) {
}

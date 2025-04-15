package com.unbumpkin.codechat.dto.social;

public record AddSlackRequest(int projectId, String workspaceId, String pat) {
}

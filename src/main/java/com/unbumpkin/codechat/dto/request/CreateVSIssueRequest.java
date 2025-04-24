package com.unbumpkin.codechat.dto.request;

    

import java.util.Map;

import com.unbumpkin.codechat.service.issuetracking.IssueTrackingService.IssueTrackingPlatforms;

public record CreateVSIssueRequest(
    String file_id, Map<String,String> attributes
) implements VectoreStoreRequestInterface {
    public CreateVSIssueRequest(String file_id, IssueTrackingPlatforms platform, String channel, String author, String issueUrl, boolean isOpen, String timestamp) {
        this(
            file_id,
            Map.of(
                "platform", platform.toString(),
                "channel", channel,
                "author", author,
                "messageUrl", issueUrl,
                "open", isOpen ? "true" : "false",
                "timestamp", timestamp
            )
        );
    }
    public Map<String, String> attributes() {
        return attributes;
    }
}

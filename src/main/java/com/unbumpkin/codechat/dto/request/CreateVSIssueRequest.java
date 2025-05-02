package com.unbumpkin.codechat.dto.request;

    

import java.util.Map;

import com.unbumpkin.codechat.service.issuetracking.IssueTrackingService.IssueTrackingPlatforms;

public record CreateVSIssueRequest(
    String file_id, Map<String,String> attributes
) implements VectoreStoreRequestInterface {
    public CreateVSIssueRequest(String file_id, IssueTrackingPlatforms platform, String channel, String author, String issueUrl, boolean isOpen, String timestamp) {
        this(
            file_id,
            createAttributes(platform, channel, author, issueUrl, isOpen, timestamp)
        );
    }
    public Map<String, String> attributes() {
        return attributes;
    }
    private static Map<String, String> createAttributes(
        IssueTrackingPlatforms platform, String channel, String author, 
        String messageUrl, boolean isOpen, String timestamp
    ) {
        Map<String, String> map = new java.util.HashMap<>();
        map.put("platform", platform.toString());
        map.put("channel", channel);
        map.put("author", author);
        map.put("messageUrl", messageUrl);
        map.put("isOpen", isOpen?"true":"false");
        map.put("timestamp", timestamp);
        return map;
    }

}

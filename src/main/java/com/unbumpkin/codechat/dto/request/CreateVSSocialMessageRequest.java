package com.unbumpkin.codechat.dto.request;

import java.util.Map;

import com.unbumpkin.codechat.service.social.SocialService.SocialPlatforms;

public record CreateVSSocialMessageRequest(
    String file_id, Map<String,String> attributes
    
) implements VectoreStoreRequestInterface {
    public CreateVSSocialMessageRequest(String file_id, SocialPlatforms platform, String channel, String author, String url, String timestamp) {
        this(
            file_id,
            createAttributes(platform, channel, author, url, timestamp)
        );
    }
    private static Map<String, String> createAttributes(SocialPlatforms platform, String channel, String author, String url, String timestamp) {
        Map<String, String> map = new java.util.HashMap<>();
        map.put("platform", platform.toString());
        map.put("channel", channel);
        map.put("author", author);
        map.put("url", url);
        map.put("timestamp", timestamp);
        return map;
    }
    public Map<String, String> attributes() {
        return attributes;
    }
}

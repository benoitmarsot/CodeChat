package com.unbumpkin.codechat.dto.request;

import java.util.Map;

import com.unbumpkin.codechat.service.social.SocialService.SocialPlatforms;

public record CreateVSSocialMessageRequest(
    String file_id, Map<String,String> attributes
    
) implements VectoreStoreRequestInterface {
    public CreateVSSocialMessageRequest(String file_id, SocialPlatforms platform, String channel, String author, String messageUrl, String timestamp) {
        this(
            file_id,
            Map.of(
                "platform", platform.toString(),
                "channel", channel,
                "author", author,
                "messageUrl", messageUrl,
                "timestamp", timestamp
            )
        );
    }
    public Map<String, String> attributes() {
        return attributes;
    }
}

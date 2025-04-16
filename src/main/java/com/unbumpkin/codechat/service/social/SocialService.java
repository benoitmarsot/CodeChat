package com.unbumpkin.codechat.service.social;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.unbumpkin.codechat.dto.social.AddSocialRequest;
import com.unbumpkin.codechat.dto.social.SocialChannel;
import com.unbumpkin.codechat.dto.social.SocialMessage;
import com.unbumpkin.codechat.dto.social.SocialUser;
import com.unbumpkin.codechat.model.ProjectResource.ResTypes;

public abstract class SocialService {
    public enum SocialPlatforms {
        slack,discord //,github,gitlab,jira,trello,zoom,teams,googlechat,notion
    }
    public static SocialService getInstance(SocialPlatforms platform, String pat, String workspaceId) {
        switch (platform) {
            case slack:
                return new SlackService(pat, workspaceId);
            case discord:
                return new DiscordService(pat, workspaceId);
            default:
                throw new IllegalArgumentException("Unsupported social platform: " + platform);
        }
    }
    public static SocialService getInstance(AddSocialRequest request) {
        return getInstance(request.platform(), request.pat(), request.workspaceId());
    }
    public abstract List<SocialUser> getUsers() throws IOException;
    public abstract List<SocialMessage> getMessages( String channel, String ts ) throws IOException;
    public abstract List<SocialChannel> getDiscussions() throws IOException;
    public abstract String getMessageUrl(SocialMessage message,SocialChannel channel);
    public abstract String getRessourceUrl();
    public abstract String getTSIso8601(SocialMessage message);
    public Map<String, SocialUser> getUsersMap() throws IOException {
        Map<String, SocialUser> userMap = new HashMap<>();
        List<SocialUser> users = getUsers();
        for (SocialUser user : users) {
            userMap.put(user.userId(), user);
        }
        return userMap;
    }
    public SocialPlatforms platform() {
        return null;
    }
    public ResTypes resType() {
        return null;
    }
}

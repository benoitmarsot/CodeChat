package com.unbumpkin.codechat.service.social;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.unbumpkin.codechat.dto.social.SocialChannel;
import com.unbumpkin.codechat.dto.social.SocialMessage;
import com.unbumpkin.codechat.dto.social.SocialUser;

public abstract class SocialServiceBase {
    public enum SocialPlatforms {
        slack,discord,github,gitlab,jira,trello,zoom,teams,googlechat,notion
    }
    public abstract List<SocialUser> getUsers() throws IOException;
    public abstract Map<String, SocialUser> getUsersMap() throws IOException;
    public abstract List<SocialMessage> getMessages( String channel, String ts ) throws IOException;
    public abstract List<SocialChannel> getDiscussions() throws IOException;
    public abstract String getMessageUrl(SocialMessage message,SocialChannel channel);
    public abstract String getRessourceUrl();
    public abstract String getTSIso8601(SocialMessage message);
}

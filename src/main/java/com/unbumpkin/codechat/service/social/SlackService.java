package com.unbumpkin.codechat.service.social;

import java.io.IOException;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.lang.String.format;

import com.slack.api.Slack;
import com.slack.api.model.Message;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import com.unbumpkin.codechat.dto.social.SocialChannel;
import com.unbumpkin.codechat.dto.social.SocialMessage;
import com.unbumpkin.codechat.dto.social.SocialUser;
import com.unbumpkin.codechat.model.ProjectResource.ResTypes;


public class SlackService extends SocialService {
    public static final String SLACK_API_URL = "https://slack.com/api/";
    
    private final String apiToken;
    private final String workspaceId;

    public SlackService(String apiToken, String workspaceId) {
        this.apiToken = apiToken;
        this.workspaceId = workspaceId;
    }

    @Override
    public List<SocialUser> getUsers() throws IOException {
        try {
            Slack slack = Slack.getInstance();
            MethodsClient methods = slack.methods(apiToken);
            UsersListResponse ulResponse = methods.usersList(r -> r.token(apiToken));
            List<SocialUser> users = ulResponse.getMembers().stream()
                    .map(user -> new SocialUser(
                        user.getId(), 
                        (user.getRealName() != null) ? user.getRealName() : user.getName(), 
                        user.getProfile().getEmail()
                    ))
                    .toList();
            if (!ulResponse.isOk()) {
                throw new RuntimeException("Error getting Slack users: " + ulResponse.getError());
            }
            return users;        
        } catch (SlackApiException e) {
            throw new RuntimeException("Error getting Slack users: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, SocialUser> getUsersMap() throws IOException {
        try {
            Map<String, SocialUser> userMap = new HashMap<>();
            List<SocialUser> users = getUsers();
            for (SocialUser user : users) {
                userMap.put(user.userId(), user);
            }
            return userMap;
        } catch (IOException e) {
            throw new RuntimeException("Error getting Slack users map: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SocialMessage> getMessages( String channel, String ts ) throws IOException {
        try {
            Slack slack = Slack.getInstance();
            MethodsClient methods = slack.methods(apiToken);
            ConversationsHistoryRequest chRequest = ConversationsHistoryRequest.builder()
                    .token(apiToken)
                    .channel(channel) // Replace with your channel ID
                    .oldest(ts) // Replace with your timestamp
                    .build();
            ConversationsHistoryResponse chResponse = methods.conversationsHistory(chRequest);
            List<Message> slackMsgs = chResponse.getMessages();
            if (!chResponse.isOk()) {
                throw new RuntimeException("Error getting Slack messages: " + chResponse.getError());
            }
            if(slackMsgs==null) {
                return List.of();
            }
            List<SocialMessage> messages = slackMsgs.stream()
                    .map(m -> new SocialMessage(m.getUser(), m.getTs(), m.getText()))
                    .toList();
            return messages;        
        } catch (SlackApiException e) {
            throw new RuntimeException("Error getting Slack messages: " + e.getMessage(), e);
        } 
    }

    @Override
    public String getMessageUrl(SocialMessage message, SocialChannel channel) {
        return format(
            "https://%s.slack.com/archives/%s/p%s"
            ,this.workspaceId, channel.channelId(),message.ts().replace(".","")
        );
    }
    @Override
    public String getTSIso8601(SocialMessage message) {
        // Slack timestamp format is "1234567890.123456"
        // Convert seconds part to an Instant and then to a Timestamp
        long epochSeconds = Long.parseLong(message.ts().substring(0, message.ts().indexOf(".")));
        Timestamp ts = Timestamp.from(Instant.ofEpochSecond(epochSeconds));

        return ts.toInstant().toString();
    }
    @Override
    public String getRessourceUrl() {
        return format(
            "https://%s.slack.com/"
            ,this.workspaceId
        );
    }

    @Override
    public List<SocialChannel> getDiscussions() throws IOException {
        try {
            Slack slack = Slack.getInstance();
            MethodsClient methods = slack.methods(apiToken);
            ConversationsListResponse clResponse = methods.conversationsList( r-> r.token(apiToken));
            List<SocialChannel> channels = clResponse.getChannels().stream()
                    .map(c -> new SocialChannel(c.getId(), c.getName(), ""))
                    .toList();
            if (!clResponse.isOk()) {
                throw new RuntimeException("Error getting Slack discussions: " + clResponse.getError());
            } 
            return channels;
        } catch (SlackApiException e) {
            throw new RuntimeException("Error getting Slack discussions: " + e.getMessage(), e);
        }
    }
    @Override
    public SocialPlatforms platform() {
        return SocialPlatforms.slack;
    }
    @Override
    public ResTypes resType() {
        return ResTypes.slack;
    }

}

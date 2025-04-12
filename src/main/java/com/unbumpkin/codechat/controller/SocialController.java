package com.unbumpkin.codechat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.request.conversations.ConversationsListRequest.ConversationsListRequestBuilder;
import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.users.UsersListResponse;

import okhttp3.ResponseBody;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@Controller
@RequestMapping("/api/v1/social")
public class SocialController {
    @GetMapping("slack-users")
    public ResponseEntity<String> getSlackUsers() {
        String token=System.getenv("SLACK_API_TOKEN");
        Slack slack = Slack.getInstance();
        MethodsClient methods = slack.methods(token);
        try {
            UsersListResponse ulResponse = methods.usersList(r -> r.token(token));
            ulResponse.getMembers().forEach(user -> {
                System.out.println("User ID: " + user.getId());
                System.out.println("User Name: " + user.getName());
                System.out.println("User Real Name: " + user.getRealName());
                System.out.println("User Email: " + user.getProfile().getEmail());
            });
            //System.out.println("Response: " + ulResponse);
            if (ulResponse.isOk()) {
                return ResponseEntity.ok("Users fetched successfully");
            } else {
                return ResponseEntity.status(500).body("Error: " + ulResponse.getError());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    @GetMapping("slack-messages")
    public ResponseEntity<String> getSlackMessages(
            @RequestParam(value = "channel", required = true) String channel,
            @RequestParam(value = "ts", required = false) String ts
    ) {
        String token=System.getenv("SLACK_API_TOKEN");
        Slack slack = Slack.getInstance();
        MethodsClient methods = slack.methods(token);
        try {
            ConversationsHistoryRequest chRequest = ConversationsHistoryRequest.builder()
                    .token(token)
                    .channel(channel) // Replace with your channel ID
                    .oldest(ts) // Replace with your timestamp
                    .build();
            ConversationsHistoryResponse chResponse = methods.conversationsHistory(chRequest);
            chResponse.getMessages().forEach(m -> {
                System.out.println("User ID: " + m.getUser());
                System.out.println("User Name: " + m.getUsername());
                System.out.println("Metadata: " + m.getMetadata());
                System.out.println("ts: " + m.getTs());
                System.out.println("Message: " + m.getText());
            });
            //System.out.println("Response: " + ulResponse);
            if (chResponse.isOk()) {
                return ResponseEntity.ok("Messages fetched successfully");
            } else {
                return ResponseEntity.status(500).body("Error: " + chResponse.getError());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }


    @GetMapping("slack-discussions")
    public ResponseEntity<String> getSlackDiscussions() {
        String token=System.getenv("SLACK_API_TOKEN");
        Slack slack = Slack.getInstance();
        MethodsClient methods = slack.methods(token);
        try {
            ConversationsListResponse clResponse = methods.conversationsList( r-> r.token(token));
            clResponse.getChannels().forEach(c -> {
                System.out.println("Channel Id: " + c.getId());
                System.out.println("Channel Name: " + c.getName());
                System.out.println("Channel Topic]: " + c.getTopic());
            });
            //System.out.println("Response: " + ulResponse);
            if (clResponse.isOk()) {
                return ResponseEntity.ok("Messages fetched successfully");
            } else {
                return ResponseEntity.status(500).body("Error: " + clResponse.getError());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    
    
}

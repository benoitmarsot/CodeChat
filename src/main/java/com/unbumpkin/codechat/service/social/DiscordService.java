package com.unbumpkin.codechat.service.social;

import java.io.IOException;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import static java.lang.String.format;

import com.unbumpkin.codechat.dto.social.SocialChannel;
import com.unbumpkin.codechat.dto.social.SocialMessage;
import com.unbumpkin.codechat.dto.social.SocialUser;
import com.unbumpkin.codechat.model.ProjectResource.ResTypes;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordService extends SocialService {
    private final String guildId;
    private final JDA jda;
    
    public DiscordService(String botToken, String guildId) {
        this.guildId = guildId;
        
        try {
            // Initialize JDA with the bot token
            this.jda = JDABuilder.createDefault(botToken)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .build();
            
            // Wait for JDA to be ready
            this.jda.awaitReady();
            for (Guild g : jda.getGuilds()) {
                System.out.println("Guild: " + g.getName() + " (" + g.getId() + ")");
            }
            Guild guild = jda.getGuildById("1352499579550957628");
            System.out.println("Guild Members: " + guild.getMemberCount());
            System.out.println("Guild Channels: " + guild.getTextChannels().size());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error initializing Discord connection: " + e.getMessage(), e);
        }
    }
    
    private Guild getGuild() {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new RuntimeException("Could not find Discord guild with ID: " + guildId);
        }
        return guild;
    }
    
    @Override
    public List<SocialUser> getUsers() throws IOException {
        try {
            Guild guild = getGuild();
            
            // Use Task directly - it doesn't have submit()
            List<Member> members = guild.loadMembers().get(); // This blocks until all members are loaded
            
            return members.stream()
                .map(member -> {
                    User user = member.getUser();
                    return new SocialUser(
                        user.getId(),
                        member.getEffectiveName(),
                        null
                    );
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IOException("Failed to load Discord members: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SocialMessage> getMessages(String channelId, String timestamp) throws IOException {
        try {
            Guild guild = getGuild();
            TextChannel channel = guild.getTextChannelById(channelId);
            
            if (channel == null) {
                throw new IOException("Could not find Discord channel with ID: " + channelId);
            }
            
            List<Message> allMessages = new ArrayList<>();
            String currentReference = timestamp;
            boolean hasMoreMessages = true;
            
            // Loop until we have no more messages or hit a reasonable limit
            while (hasMoreMessages ) { // Limiting to 1000 messages max to prevent excessive API calls
                MessageHistory history;
                
                if (currentReference != null && !currentReference.equals("0") && !currentReference.isEmpty()) {
                    // Get messages before the specified message ID
                    long messageId = Long.parseLong(currentReference);
                    history = channel.getHistoryBefore(messageId, 100).complete();
                } else if (allMessages.isEmpty()) {
                    // First batch of recent messages
                    history = MessageHistory.getHistoryFromBeginning(channel).limit(100).complete();
                } else {
                    // We've already fetched the first batch but have no reference - we're done
                    break;
                }
                
                List<Message> retrievedMessages = history.getRetrievedHistory();
                
                // If we got back fewer than 100 messages, there are no more to fetch
                if (retrievedMessages.size() < 100) {
                    hasMoreMessages = false;
                }
                
                // Add the retrieved messages to our result list
                allMessages.addAll(retrievedMessages);
                
                // Update reference to the oldest message ID for the next iteration
                if (!retrievedMessages.isEmpty() && hasMoreMessages) {
                    Message oldestMessage = retrievedMessages.get(retrievedMessages.size() - 1);
                    currentReference = oldestMessage.getId();
                } else {
                    hasMoreMessages = false;
                }
            }
            
            // Convert Discord messages to SocialMessage objects
            return allMessages.stream()
                .map(message -> new SocialMessage(
                    message.getAuthor().getId(),
                    message.getId(),
                    message.getContentDisplay()
                ))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            throw new IOException("Error getting Discord messages: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SocialChannel> getDiscussions() throws IOException {
        try {
            Guild guild = getGuild();
            
            return guild.getTextChannels().stream()
                .map(channel -> new SocialChannel(
                    channel.getId(),
                    channel.getName(),
                    channel.getTopic() != null ? channel.getTopic() : ""
                ))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            throw new IOException("Error getting Discord channels: " + e.getMessage(), e);
        }
    }

    @Override
    public String getMessageUrl(SocialMessage message, SocialChannel channel) {
        return format(
            "https://discord.com/channels/%s/%s/%s",
            this.guildId,
            channel.channelId(),
            message.ts() // In Discord, the message timestamp is its ID
        );
    }

    @Override
    public String getRessourceUrl() {
        return format(
            "https://discord.com/channels/%s",
            this.guildId
        );
    }

    @Override
    public String getTSIso8601(SocialMessage message) {
        // Discord uses Snowflake IDs which encode timestamp information
        // Extract the timestamp from the Snowflake ID
        long discordId = Long.parseLong(message.ts());
        long timestamp = ((discordId >> 22) + 1420070400000L); // Discord epoch (2015-01-01)
        
        Timestamp ts = new Timestamp(timestamp);
        return ts.toInstant().toString();
    }
    @Override
    public SocialPlatforms platform() {
        return SocialPlatforms.discord;
    }
    @Override
    public ResTypes resType() {
        return ResTypes.discord;
    }

}
package com.unbumpkin.codechat.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import com.unbumpkin.codechat.dto.openai.SocialAssistant;
import com.unbumpkin.codechat.dto.request.CreateVSSocialMessageRequest;
import com.unbumpkin.codechat.dto.social.AddSlackRequest;
import com.unbumpkin.codechat.dto.social.SocialChannel;
import com.unbumpkin.codechat.dto.social.SocialMessage;
import com.unbumpkin.codechat.dto.social.SocialUser;
import com.unbumpkin.codechat.model.ProjectResource;
import com.unbumpkin.codechat.model.UserSecret;
import com.unbumpkin.codechat.model.ProjectResource.ResTypes;
import com.unbumpkin.codechat.model.UserSecret.Labels;
import com.unbumpkin.codechat.model.openai.OaiFile;
import com.unbumpkin.codechat.model.openai.VectorStore;
import com.unbumpkin.codechat.model.openai.OaiFile.Purposes;
import com.unbumpkin.codechat.repository.ProjectResourceRepository;
import com.unbumpkin.codechat.repository.openai.SocialAssistantRepository;
import com.unbumpkin.codechat.repository.openai.VectorStoreRepository;
import com.unbumpkin.codechat.repository.openai.VectorStoreRepository.RepoVectorStoreResponse;
import com.unbumpkin.codechat.repository.social.SocialChannelRepository;
import com.unbumpkin.codechat.repository.social.SocialUserRepository;
import com.unbumpkin.codechat.service.openai.AssistantBuilder;
import com.unbumpkin.codechat.service.openai.AssistantBuilder.ReasoningEfforts;
import com.unbumpkin.codechat.service.openai.AssistantService;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;
import com.unbumpkin.codechat.service.openai.CCProjectFileManager.Types;
import com.unbumpkin.codechat.service.openai.OaiFileService;
import com.unbumpkin.codechat.service.openai.VectorStoreFile;
import com.unbumpkin.codechat.service.openai.VectorStoreService;

import static com.unbumpkin.codechat.service.social.SlackService.SLACK_API_URL;
import com.unbumpkin.codechat.service.social.SlackService;
import com.unbumpkin.codechat.service.social.SocialServiceBase;
import com.unbumpkin.codechat.service.social.SocialServiceBase.SocialPlatforms;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Controller
@RequestMapping("/api/v1/social")
public class SocialController {

    @Autowired
    private OaiFileService oaiFileService;
    @Autowired
    private ProjectResourceRepository projectResourceRepository;
    @Autowired
    private SocialAssistantRepository socialAssistantRepository;
    @Autowired
    private AssistantService assistantService;
    @Autowired 
    private VectorStoreRepository vsRepository;
    @Autowired 
    private VectorStoreService vsService;
    @Autowired
    private SocialUserRepository socialUserRepository;
    @Autowired
    private SocialChannelRepository socialChannelRepository;


    @Transactional
    @PostMapping("add-slack")
    public ResponseEntity<ProjectResource> addRepoResource(
        @RequestBody AddSlackRequest request
    ) throws Exception {
        int projectId=request.projectId();
        // create the slack service
        SlackService slackService = new SlackService(request.pat(), request.workspaceId());
        // Get the project
        Integer prId=projectResourceRepository.getResourceId(projectId, slackService.getRessourceUrl());

        if(prId!=null){
            throw new Exception("The project already has the slack resource "+slackService.getRessourceUrl()+", use refresh to update it");
        }
        // Create a new project resource for SLACK_API_URL
        Map<Labels,UserSecret> userSecrets = new HashMap<>();
        if(request.pat()!=null && !request.pat().isEmpty()){
            userSecrets.put(Labels.pat, new UserSecret(Labels.pat, request.pat()));
        } else {
            throw new Exception("A personal access token is required");
        }

        ProjectResource pr=projectResourceRepository.createResource(projectId, SLACK_API_URL, ResTypes.slack, userSecrets);
        prId=pr.prId();
        VectorStore vs=getOrCreateVectorStore(projectId);
        VectorStoreFile vsf=new VectorStoreFile(vs.getOaiVsid());

        Map<String,SocialUser> mUsers=slackService.getUsersMap();
        socialUserRepository.addMissingSocialUsers(mUsers.values(),prId);
        // add the slack channels
        List<SocialChannel> sChannels=slackService.getDiscussions();
        List<SocialChannel> sChannelsDb=new ArrayList<>(sChannels.size());
        // add the slack messages
        for(SocialChannel sChannel:sChannels) {
            try{ 
                System.out.println("Getting messages for channel "+sChannel.channelName());
                List<SocialMessage> sMessages=slackService.getMessages(sChannel.channelId(), sChannel.lastMessageTs());
                addMessages( slackService,sMessages,sChannel,mUsers,pr.prId(),vsf);
                sChannelsDb.add(new SocialChannel(sChannel.channelId(),sChannel.channelName(),sMessages.get(0).ts()));
            } catch (Exception e) {
                System.out.println("Error getting messages for channel "+sChannel.channelName()+": "+e.getMessage());

            }
        }
        // add the channel to the vector store
        socialChannelRepository.addMissingSocialChannels(sChannels,prId);

        //Create a new social assistant
        int assistantId=createAssistant("Social Assisant",projectId,vs);
        System.out.println("Assistant created with id: "+assistantId);
        return ResponseEntity.ok(pr);
    }
    private VectorStore getOrCreateVectorStore(int projectId) throws IOException {
        RepoVectorStoreResponse repoVs=vsRepository.getSocialVectorStoreByProjectId(projectId);
        if(repoVs==null){
            Types type=Types.social;
            String vsName = "vs"+type.name();
            String vsDesc = "contain the "+type.name()+" files in the project.";
            VectorStore vs = new VectorStore( 0,"", projectId,vsName,vsDesc, null,type);
            String vsOaiId = vsService.createVectorStore(vs);
            vs = new VectorStore(0, vsOaiId, projectId, vsName, vsDesc, null, type);
            vs = new VectorStore( vsRepository.storeVectorStore(vs),
                vsOaiId, projectId, vsName, vsDesc, null, type);
            System.out.println("Empty vector store "+type.name()+" created with id: "+vs.getVsid()+" and OaiId: "+vsOaiId);
            return vs;
        }
        
        return new VectorStore(repoVs.id(), repoVs.vsid(), projectId, repoVs.name(), repoVs.description(), repoVs.dayskeep(), repoVs.type());
    }

    private static final Pattern NAME_REGEXP = java.util.regex.Pattern.compile("<@([A-Z0-9]+)>");
    private void addMessages(
        SocialServiceBase socialService, List<SocialMessage> sMessages, SocialChannel sChannel, Map<String,SocialUser> mUsers, int prId, VectorStoreFile vsf
    ) throws IOException {

        for(SocialMessage sMessage:sMessages){
            try {
                String message=sMessage.message();

                if(message==null || message.isEmpty()){
                    System.out.println("Message is empty, skipping it");
                    continue;
                }
                // Replace user mentions with real names
                if (message.contains("<@")) {
                    // Find user mentions pattern <@USERID>

                    java.util.regex.Matcher matcher = NAME_REGEXP.matcher(message);
                    StringBuffer sb = new StringBuffer();
                    
                    while (matcher.find()) {
                        String userId = matcher.group(1);
                        SocialUser user = mUsers.get(userId);
                        String replacement = user != null ? user.fullName() : matcher.group(0);
                        matcher.appendReplacement(sb, replacement.replace("$", "\\$"));
                    }
                    matcher.appendTail(sb);
                    message = sb.toString();
                }
                String msgUrl=socialService.getMessageUrl(sMessage, sChannel);
                CreateVSSocialMessageRequest creaVsRequest=new CreateVSSocialMessageRequest(
                    "", SocialPlatforms.slack, sChannel.channelName(),mUsers.get(sMessage.userId()).fullName(), 
                    msgUrl,socialService.getTSIso8601(sMessage)
                );
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
                message=objectMapper.writeValueAsString(CreateSocialMessage(creaVsRequest.attributes(),message));
                System.out.println("Message "+message);
                sMessage=new SocialMessage(sMessage.userId(), sMessage.ts(), message);

                OaiFile oaiFile=oaiFileService.uploadMessage(sMessage, msgUrl, Purposes.assistants, prId);
                creaVsRequest= new CreateVSSocialMessageRequest(oaiFile.fileId(),creaVsRequest.attributes());
                vsf.addFile( creaVsRequest);
                System.out.println("Slack message "+msgUrl+" added to social vector store.");
            } catch (Exception e) {
                System.out.println("Error adding message "+sMessage.message()+": "+e.getMessage());
            }
        }
    }
    private Map<String,Object> CreateSocialMessage(Map<String,String> attributes, String message) {
        Map<String,Object> map=new LinkedHashMap<>();
        map.put("attributes", attributes);
        map.put("message", message);
        return map;
    }
    int createAssistant(String name, int projectId, VectorStore vectorStore) throws IOException {
        //Create a new social assistant
        Models model=Models.gpt_4o;
        AssistantBuilder assistantBuilder = new AssistantBuilder(model);
        String instruction="""
            ## 🧠 Assistant Instructions

            <Function: You are a smart, multi-platform message search assistant. You have access to a vector store of all internal and external communications across platforms like Slack, Discord, MS Teams, and Facebook. Your job is to help users find important messages, decisions, or patterns across these platforms using natural language queries.>
            Each message contains metadata such as `platform`, `channel`, `author`, `messageUrl`, and `timestamp` (in ISO 8601 format).

            ---

            ### 🗂️ Core Function

            Your goal is to:
            - Interpret the user's query
            - Understand any time-based constraints (e.g. "last week", "Q1", "yesterday")
            - Convert natural language into filters (timestamp, channel, author, etc.)
            - Search for the most relevant messages across platforms
            - Summarize, answer
            - reference relevant discussion messages

            Use the `timestamp` field for any time-based filtering. Timestamps are in ISO 8601 format.

            ---

            ### 🗓️ Time Filter Handling

            Translate natural language into clear date ranges:

            | Expression                    | Date Range (ISO 8601)                 |
            |------------------------------|----------------------------------------|
            | "yesterday"                  | Previous day, 00:00–23:59             |
            | "last week"                  | Last Monday to last Sunday            |
            | "last month"                 | First to last day of previous month   |
            | "from January to March"      | Jan 1 to Mar 31 (same year unless stated) |
            | "between March 1 and 31"     | Mar 1 to Mar 31                       |
            | "last 7 days"                | Today - 7 days to today               |

            ---

            ### 💬 Conversational Guidance

            When responding to users:

            - Be natural, clear, and helpful — sound like a human teammate who understands context.
            - Ask clarifying questions when queries are ambiguous (e.g., "Did you mean March of this year or last year?")
            ---
            """;
        assistantBuilder.setName(name)
            .setDescription("Code search assistant for " + name)
            .setInstructions(instruction).setReasoningEffort(ReasoningEfforts.high)
            //.setTemperature(.02) //Not suported in o3-mini
            .addFileSearchTool().addFileSearchAssist()
            .setFileSearchMaxNumResults(20) //default
            //.setFileSearchRankingOption(.5) 
            .setToolResourcesFileSearch(Set.of(vectorStore.getOaiVsid()))
            ;
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println(mapper.writeValueAsString(assistantBuilder));

        String assistantOaiId=assistantService.createAssistant(assistantBuilder);
        SocialAssistant assistant = new SocialAssistant(
            0, assistantOaiId, projectId, name, "Social search assistant for " + name,
            instruction, ReasoningEfforts.high, model, .7f, 10, 
            vectorStore.getVsid()
        );
        return socialAssistantRepository.addAssistant(assistant);
    }

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

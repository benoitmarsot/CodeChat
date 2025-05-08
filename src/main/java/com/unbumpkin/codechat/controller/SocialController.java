package com.unbumpkin.codechat.controller;

import java.io.IOException;
import java.net.URLEncoder;
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
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.unbumpkin.codechat.ai.chunker.TextChunker;
import com.unbumpkin.codechat.ai.dto.Chunk;
import com.unbumpkin.codechat.ai.dto.EmbeddedChunk;
import com.unbumpkin.codechat.ai.dto.SearchChunkResult;
import com.unbumpkin.codechat.ai.embedder.EmbedderService;
import com.unbumpkin.codechat.ai.vectorstore.PgVectorRepository;
import com.unbumpkin.codechat.dto.FindInPgVectorRequest;
import com.unbumpkin.codechat.dto.issuetracker.AddIssueTrackerRequest;
import com.unbumpkin.codechat.dto.issuetracker.Issue;
import com.unbumpkin.codechat.dto.openai.SocialAssistant;
import com.unbumpkin.codechat.dto.request.CreateVSIssueRequest;
import com.unbumpkin.codechat.dto.request.CreateVSSocialMessageRequest;
import com.unbumpkin.codechat.dto.social.AddSocialRequest;
import com.unbumpkin.codechat.dto.social.SocialChannel;
import com.unbumpkin.codechat.dto.social.SocialMessage;
import com.unbumpkin.codechat.dto.social.SocialUser;
import com.unbumpkin.codechat.model.ProjectResource;
import com.unbumpkin.codechat.model.UserSecret;
import com.unbumpkin.codechat.model.UserSecret.Labels;
import com.unbumpkin.codechat.model.openai.OaiFile;
import com.unbumpkin.codechat.model.openai.VectorStore;
import com.unbumpkin.codechat.model.openai.OaiFile.Purposes;
import com.unbumpkin.codechat.repository.ProjectRepository;
import com.unbumpkin.codechat.repository.ProjectResourceRepository;
import com.unbumpkin.codechat.repository.openai.SocialAssistantRepository;
import com.unbumpkin.codechat.repository.openai.VectorStoreRepository;
import com.unbumpkin.codechat.repository.openai.VectorStoreRepository.RepoVectorStoreResponse;
import com.unbumpkin.codechat.repository.social.SocialChannelRepository;
import com.unbumpkin.codechat.repository.social.SocialUserRepository;
import com.unbumpkin.codechat.service.issuetracking.IssueTrackingService;
import com.unbumpkin.codechat.service.openai.AssistantBuilder;
import com.unbumpkin.codechat.service.openai.AssistantBuilder.ReasoningEfforts;
import com.unbumpkin.codechat.service.openai.AssistantService;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;
import com.unbumpkin.codechat.service.openai.CCProjectFileManager.Types;
import com.unbumpkin.codechat.service.openai.OaiFileService;
import com.unbumpkin.codechat.service.openai.VectorStoreFile;
import com.unbumpkin.codechat.service.openai.VectorStoreService;

import com.unbumpkin.codechat.service.social.SocialService;
import com.unbumpkin.codechat.service.social.SocialService.SocialPlatforms;

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
    @Autowired
    private PgVectorRepository pgVectorRepository;
    @Autowired
    private TextChunker textChunker;
    @Autowired
    private EmbedderService embeddingService;
    @Autowired
    private ProjectRepository projectRepository;

    @PostMapping("find-in-pgvector")
    public ResponseEntity<List<String>> findInPgVector(
        @RequestBody FindInPgVectorRequest request
    ) throws Exception {
        if(projectRepository.getProjectById(request.projectId())==null){
            // Project not found
            return ResponseEntity.badRequest().body(List.of("Project not found"));
        }
        if(!pgVectorRepository.haveContent(request.projectId(), request.type())){
            // No content in the vector store for the projectId and type
            return ResponseEntity.ok(List.of());
        }
        float[] embededQusetion=embeddingService.embed(request.content());

        List<SearchChunkResult> chunks=pgVectorRepository.search(request.projectId(), request.type(), embededQusetion, 10, null);

        List<String> answers=new ArrayList<>(chunks.size());
        chunks.forEach(chunk -> answers.add(chunk.content()));
        return ResponseEntity.ok(answers);
    }
    


    @Transactional
    @PostMapping("add-issue-tracker")
    public ResponseEntity<ProjectResource> addSocialIssueTracker(
        @RequestBody AddIssueTrackerRequest request
    ) throws Exception {
        int projectId=request.projectId();
        // create the IsuuseTracking service
        IssueTrackingService issueTrackingService = IssueTrackingService.getInstance(request);
        // Get the project resource
        Integer prId=projectResourceRepository.getResourceId(projectId, issueTrackingService.getRessourceUrl());
        if(prId!=null){
            //TODO: implement refresh
            throw new Exception("The project already has the issue tracker resource "+issueTrackingService.getRessourceUrl()+", use refresh to update it");
        }
        // Create a new project resource 
        Map<Labels,UserSecret> userSecrets = new HashMap<>();
        if(request.pat()!=null && !request.pat().isEmpty()){
            userSecrets.put(Labels.pat, new UserSecret(Labels.pat, request.pat()));
        }
        if(request.userName()!=null && !request.userName().isEmpty()){
            userSecrets.put(Labels.username, new UserSecret(Labels.username, request.userName()));
        }
        if(request.password()!=null && !request.password().isEmpty()){
            userSecrets.put(Labels.password, new UserSecret(Labels.password, request.password()));
        }
        ProjectResource pr=projectResourceRepository.createResource(
            projectId, issueTrackingService.getRessourceUrl(), issueTrackingService.resType(), userSecrets
        );
        prId=pr.prId();

        VectorStore vs=null;
        VectorStoreFile vsf=null;
        if(!request.selfChunk()) {
            vs=getOrCreateVectorStore(projectId);
            vsf=new VectorStoreFile(vs.getOaiVsid());
        }

        Map<String,SocialUser> mUsers=issueTrackingService.getUsersMap();
        socialUserRepository.addMissingSocialUsers(mUsers.values(),prId);

        // add the issues
        //Getting all issues
        System.out.println("Getting all issues for project "+request.repositoryName());
        List<Issue> issues=issueTrackingService.getAllIssues();
        //String stLastUpdated=issues.get(0).lastUpdated();
        //Transform iso8601 date to milli
        

        if(!request.selfChunk()) {
            // add the messages to the openai vector store
            addIssues(issueTrackingService,issues,mUsers,pr.prId(),vsf);
        } else {
            // add the messages to the PgVector store
            addIssues(issueTrackingService,issues,mUsers,pr.prId(),projectId);
        }


        List<SocialChannel> sChannelsDb=new ArrayList<>(1);
        sChannelsDb.add(new SocialChannel(
            issueTrackingService.getIssuesChannel(),issueTrackingService.getIssuesChannel(),issues.get(0).lastUpdated()
        ));
        
        socialChannelRepository.addMissingSocialChannels(sChannelsDb,prId);

        if(!request.selfChunk()) {
            //Create a new social assistant if needed
            int assistantId=retrieveCreateAssistant("Social Assisant",projectId,vs);
            System.out.println("Assistant created with id: "+assistantId);
        }
        return ResponseEntity.ok(pr);
    }
    @Transactional
    @PostMapping("add-social-platform")
    public ResponseEntity<ProjectResource> addSocialResource(
        @RequestBody AddSocialRequest request
    ) throws Exception {
        int projectId=request.projectId();
        // Get the social service
        SocialService socialService = SocialService.getInstance(request);
        // Get the project resource
        Integer prId=projectResourceRepository.getResourceId(projectId, socialService.getRessourceUrl());

        if(prId!=null){
            //TODO: implement refresh
            throw new Exception("The project already has the social resource "+socialService.getRessourceUrl()+", use refresh to update it");
        }
        // Create a new project resource 
        Map<Labels,UserSecret> userSecrets = new HashMap<>();
        if(request.pat()!=null && !request.pat().isEmpty()){
            userSecrets.put(Labels.pat, new UserSecret(Labels.pat, request.pat()));
        } else {
            throw new Exception("A personal access token is required");
        }

        ProjectResource pr=projectResourceRepository.createResource(projectId, socialService.getRessourceUrl(), socialService.resType(), userSecrets);
        prId=pr.prId();

        Map<String,SocialUser> mUsers=socialService.getUsersMap();
        socialUserRepository.addMissingSocialUsers(mUsers.values(),prId);
        // add the social channels
        List<SocialChannel> sChannels=socialService.getDiscussions();
        List<SocialChannel> sChannelsDb=new ArrayList<>(sChannels.size());

        VectorStore vs=null;
        VectorStoreFile vsf=null;
        if(!request.selfChunk()) {
            vs=getOrCreateVectorStore(projectId);
            vsf=new VectorStoreFile(vs.getOaiVsid());
        }
        // add the social messages
        for(SocialChannel sChannel:sChannels) {
            try{ 
                System.out.println("Getting messages for channel "+sChannel.channelName());
                List<SocialMessage> sMessages=socialService.getMessages(sChannel.channelId(), sChannel.lastMessageTs());
                if(!request.selfChunk()) {
                    // add the messages to the openai vector store
                    addMessages(socialService,sMessages,sChannel,mUsers,pr.prId(),vsf);
                } else {
                    // add the messages to the PgVector store
                    addMessages(socialService, sMessages, sChannel, mUsers, projectId);
                }
                sChannelsDb.add(new SocialChannel(sChannel.channelId(),sChannel.channelName(),sMessages.get(0).ts()));
            } catch (Exception e) {
                System.out.println("Error getting messages for channel "+sChannel.channelName()+": "+e.getMessage());

            }
        }
        // add the channel to the vector store
        socialChannelRepository.addMissingSocialChannels(sChannelsDb,prId);

        //TODO: add the last message timestamp to the socialchannel
        //Create a new social assistant
        if(!request.selfChunk()) {
            int assistantId=retrieveCreateAssistant("Social Assisant",projectId,vs);
            System.out.println("Assistant created with id: "+assistantId);
        }
        return ResponseEntity.ok(pr);
    }
    /**
     * Add the messages to the PgVector local vector store
     * @param socialService 
     * @param sMessages
     * @param sChannel
     * @param mUsers
     * @param prId
     * @throws IOException
     */
    private void addMessages(
        SocialService socialService, List<SocialMessage> sMessages, SocialChannel sChannel, 
        Map<String,SocialUser> mUsers, int projectId
    ) throws IOException {
        //Chunk the messages
        for(SocialMessage sMessage:sMessages){
            try {
                String message=sMessage.message();

                if(message==null || message.isEmpty()){
                    System.out.println("Message is empty, skipping it");
                    continue;
                }
                //Replace the user id with the user name
                message=replaceUserIdWithName(message, mUsers, socialService);
                String msgUrl=socialService.getMessageUrl(sMessage, sChannel);
                CreateVSSocialMessageRequest creaVsRequest=new CreateVSSocialMessageRequest(
                    "", socialService.platform(), sChannel.channelName(),mUsers.get(sMessage.userId()).fullName(), 
                    msgUrl,socialService.getTSIso8601(sMessage)
                );
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
                //message=objectMapper.writeValueAsString(CreateSocialMessage(creaVsRequest.attributes(),message));
                System.out.println("Message "+message);
                //sMessage=new SocialMessage(sMessage.userId(), sMessage.ts(), message);
                List<Chunk> chunks = textChunker.chunk(message, "txt", creaVsRequest.attributes());
                List<EmbeddedChunk> embedded = embeddingService.embedChunks(chunks);
                pgVectorRepository.saveAll( projectId, Types.social, embedded);

                System.out.println(socialService.platform().toString()+" self chunk message "+msgUrl+" added to social chunks vector store.");
            } catch (Exception e) {
                System.out.println("Error adding message "+sMessage.message()+": "+e.getMessage());
            }
        }
    }
    @SuppressWarnings("unused")
    private String getMsgFName(SocialMessage sMessage, SocialChannel sChannel) {
        try {
            String fName = URLEncoder.encode(sChannel.channelName() + "_author_" + sMessage.ts() + ".txt", "UTF-8");
            return fName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode message filename", e);
        }
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
    
    private void addIssues(
        IssueTrackingService issueTrackingService, List<Issue> issues, Map<String,SocialUser> mUsers, int prId, VectorStoreFile vsf
    ) throws IOException {
        for(Issue issue:issues){
            try {
                String body=issue.body();
                if(body==null || body.isEmpty()){
                    System.out.println("Message is empty, skipping it");
                    continue;
                }
                String issueUrl=issue.url(); // maybe needs issueTrackingService.getIssueUrl(issue.issueNumber());
                CreateVSIssueRequest creaVsRequest=new CreateVSIssueRequest(
                    "", issueTrackingService.platform(), issueTrackingService.getIssuesChannel(),mUsers.get(issue.author()).fullName(), 
                    issueUrl,issue.isOpen(),issue.lastUpdated()
                );
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
                body=objectMapper.writeValueAsString(CreateSocialMessage(creaVsRequest.attributes(),body));
                System.out.println("body: "+body);
                issue=new Issue(issue, body);

                OaiFile oaiFile=oaiFileService.uploadIssue(issue, issueUrl, Purposes.assistants, prId);
                creaVsRequest= new CreateVSIssueRequest(oaiFile.fileId(),creaVsRequest.attributes());
                vsf.addFile( creaVsRequest);
                System.out.println(issueTrackingService.platform().toString()+" message "+issueUrl+" added to social vector store.");
            } catch (Exception e) {
                System.out.println("Error adding message "+issue.Title()+": "+e.getMessage());
            }
        }
    }
    private void addIssues(
        IssueTrackingService issueTrackingService, List<Issue> issues, Map<String,SocialUser> mUsers, int prId, int projectId
    ) throws Exception {
        for (Issue issue : issues) {
            try {
                String body = issue.body();
                if (body == null || body.isEmpty()) {
                    System.out.println("Issue body is empty, skipping it");
                    continue;
                }
                String issueUrl = issue.url();
                CreateVSIssueRequest creaVsRequest = new CreateVSIssueRequest(
                    "", issueTrackingService.platform(), issueTrackingService.getIssuesChannel(),
                    mUsers.get(issue.author()).fullName(),
                    issueUrl, issue.isOpen(), issue.lastUpdated()
                );
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
                //body = objectMapper.writeValueAsString(CreateSocialMessage(creaVsRequest.attributes(), body));
                System.out.println("body: " + body);
    
                //String issueFName = getIssueFName(issue);
                // Chunk and embed
                List<Chunk> chunks = textChunker.chunk(body, "txt", creaVsRequest.attributes());
                List<EmbeddedChunk> embedded = embeddingService.embedChunks(chunks);
                pgVectorRepository.saveAll(projectId, Types.social, embedded);
    
                System.out.println(issueTrackingService.platform().toString() + " self chunk issue " + issueUrl + " added to social chunks vector store.");
            } catch (Exception e) {
                System.out.println("Error adding issue " + issue.Title() + ": " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }
    }
    @SuppressWarnings("unused")
    private String getIssueFName(Issue issue) {
        String fName = "issue_" + issue.number() + ".txt";
        return fName;
    }

    private static final Pattern NAME_REGEXP = java.util.regex.Pattern.compile("<@([A-Z0-9]+)>");
    private void addMessages(
        SocialService socialService, List<SocialMessage> sMessages, SocialChannel sChannel, 
        Map<String,SocialUser> mUsers, int prId, VectorStoreFile vsf
    ) throws IOException {

        for(SocialMessage sMessage:sMessages){
            try {
                String message=sMessage.message();

                if(message==null || message.isEmpty()){
                    System.out.println("Message is empty, skipping it");
                    continue;
                }
                //Replace the user id with the user name
                message=replaceUserIdWithName(message, mUsers, socialService);
                String msgUrl=socialService.getMessageUrl(sMessage, sChannel);
                CreateVSSocialMessageRequest creaVsRequest=new CreateVSSocialMessageRequest(
                    "", socialService.platform(), sChannel.channelName(),mUsers.get(sMessage.userId()).fullName(), 
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
                System.out.println(socialService.platform().toString()+" message "+msgUrl+" added to social vector store.");
            } catch (Exception e) {
                System.out.println("Error adding message "+sMessage.message()+": "+e.getMessage());
            }
        }
    }
    private String replaceUserIdWithName(String message, Map<String,SocialUser> mUsers, SocialService socialService) {
        // Replace user mentions with real names
        if (message.contains("<@") && socialService.platform() == SocialPlatforms.slack) {
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
        return message;
    }
    private Map<String,Object> CreateSocialMessage(Map<String,String> attributes, String message) {
        Map<String,Object> map=new LinkedHashMap<>();
        map.put("attributes", attributes);
        map.put("message", message);
        return map;
    }
    private int retrieveCreateAssistant(String name, int projectId, VectorStore vectorStore) throws IOException {
        //Check if the assistant already exists
        SocialAssistant assistant=socialAssistantRepository.getAssistantByProjectId(projectId);
        if(assistant!=null) {
            System.out.println("Assistant retrieved with id: "+assistant.aid());

            return assistant.aid();
        }
        //Create a new social assistant
        Models model=Models.gpt_4o;
        AssistantBuilder assistantBuilder = new AssistantBuilder(model);
        String instruction="""
            ## 🧠 Assistant Instructions

            <Function: You are a smart, multi-platform message search assistant. You have access to a vector store of all internal and external communications across platforms like Slack, Discord, MS Teams, and Facebook. Your job is to help users find important messages, decisions, or patterns across these platforms using natural language queries.>
            Each documents (messages or tickets) contains metadata such as `platform`, `channel`, `author`, `url`, and `timestamp` (in ISO 8601 format).

            ---

            ### 🗂️ Core Function

            Your goal is to:
            - Interpret the user's query
            - Understand any time-based constraints (e.g. "last week", "Q1", "yesterday")
            - Convert natural language into filters (timestamp, channel, author, etc.)
            - Search for the most relevant documents across platforms
            - Summarize, answer
            - reference relevant documents

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
        assistant = new SocialAssistant(
            0, assistantOaiId, projectId, name, "Social search assistant for " + name,
            instruction, ReasoningEfforts.high, model, .7f, 10, 
            vectorStore.getVsid()
        );
        System.out.println("Assistant created with id: "+assistant.aid());

        return socialAssistantRepository.addAssistant(assistant);
    }

}

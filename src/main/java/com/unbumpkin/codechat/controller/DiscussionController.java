package com.unbumpkin.codechat.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.unbumpkin.codechat.dto.openai.Assistant;
import com.unbumpkin.codechat.dto.openai.SocialAssistant;
import com.unbumpkin.codechat.dto.request.AddOaiThreadRequest;
import com.unbumpkin.codechat.dto.request.DiscussionNameSuggestion;
import com.unbumpkin.codechat.dto.request.DiscussionUpdateRequest;
import com.unbumpkin.codechat.dto.request.MessageCreateRequest;
import com.unbumpkin.codechat.model.Discussion;
import com.unbumpkin.codechat.model.Message;
import com.unbumpkin.codechat.model.openai.OaiThread;
import com.unbumpkin.codechat.repository.DiscussionRepository;
import com.unbumpkin.codechat.repository.MessageRepository;
import com.unbumpkin.codechat.repository.openai.AssistantRepository;
import com.unbumpkin.codechat.repository.openai.OaiFileRepository;
import com.unbumpkin.codechat.repository.openai.OaiThreadRepository;
import com.unbumpkin.codechat.repository.openai.SocialAssistantRepository;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Roles;
import com.unbumpkin.codechat.service.openai.CCProjectFileManager.Types;
import com.unbumpkin.codechat.service.openai.ChatService;
import com.unbumpkin.codechat.service.openai.OaiMessageService;
import com.unbumpkin.codechat.service.openai.OaiRunService;
import com.unbumpkin.codechat.service.openai.OaiThreadService;

@RestController
@RequestMapping("/api/v1/discussions")
public class DiscussionController {

    @Autowired
    private DiscussionRepository discussionRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private AssistantRepository assistantRepository;
    @Autowired
    private SocialAssistantRepository socialAssistantRepository;
    @Autowired
    private OaiThreadService threadService;
    @Autowired
    private OaiThreadRepository threadRepository;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired 
    OaiThreadService oaiThreadService;
    @Autowired
    OaiFileRepository oaiFileRepository;



    @PostMapping
    public ResponseEntity<Discussion> createDiscussion(
        @RequestBody Discussion discussionRequest
    ) throws IOException {
        Discussion discussion=discussionRepository.addDiscussion(discussionRequest);
        Assistant assistant=assistantRepository.getAssistantByProjectId(
            discussion.projectId(), discussionRequest.assistantType());
        String oaiThreadId=threadService.createThread();
        System.out.println("OpenAi thread " + oaiThreadId+" created...");
        threadRepository.addThread(new AddOaiThreadRequest(oaiThreadId, assistant.fullvsid(),discussion.did(), "all"));

        return ResponseEntity.ok(discussion);
    }
    @PostMapping("/ask-question")
    public ResponseEntity<Message> askQuestion(
        @RequestBody MessageCreateRequest request
    ) throws IOException {
        try {
            Message returnedMessage=messageRepository.addMessage(request);
            Discussion discussion=discussionRepository.getDiscussionById(returnedMessage.discussionId());
            Map<Types,OaiThread> threadMap=threadRepository.getAllThreadsByDiscussionId(discussion.did());
            OaiThread thread=threadMap.get(Types.all);
            OaiMessageService messageService=new OaiMessageService(thread.oaiThreadId());
            String oaiMsgId=messageService.createMessage(Roles.user,returnedMessage.message());
            System.out.println("OpenAi message " + oaiMsgId+" created...");
            return ResponseEntity.ok(returnedMessage);
        } catch (Exception e) {
            System.out.println("exception in askQuestion: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    private String askSocialAssistantInsights(String messageTxt, Discussion discussion) throws IOException {
        try {
            // Get all messages for the discussion 
            List<Message> messages = messageRepository.getAllMessagesByDiscussionId(discussion.did());
            
            Message sysMessage = new Message( 0,
                discussion.did(),
                Roles.user.toString(), 0,
                "[SYSTEM INSTRUCTION] \n" +
                "Most importantly, Only answer based on the results from vector store. If no match is found, answer with empty string. \n"+
                "When a user sends a query, treat the full conversation thread from the main assistant as the query context. Use that entire thread (not just the latest message) to search the attached vector store, which contains Slack/Discord messages and Jira/GitHub tickets.\n" +
                "Search using the original full joined terms (e.g., 'authToken', 'issue_id'), giving the highest weight to exact matches of these joined words.\n" +
                "To improve retrieval accuracy, preprocess the thread content before searching:\n" +
                "- Also split compound or joined words (e.g., camelCase, PascalCase, snake_case, kebab-case) into individual tokens. For example, 'authToken' becomes 'auth' and 'token'; 'issue_id' becomes 'issue' and 'id'.\n" +
                "Return the most relevant results that semantically match the thread. Include references (e.g., URLs, message snippets, or ticket IDs) to the original content in your responses.\n"
                ,null
            );
            messages.add(0, sysMessage);
            // Create a new thread with all messages at once
            String socialThreadId = threadService.createThreadWithMessages(messages);
            
            // Run the social assistant on this thread
            SocialAssistant socialAssistant = socialAssistantRepository.getAssistantByProjectId(
                discussion.projectId()
            );
            
            OaiRunService runService = new OaiRunService(socialAssistant.oaiAid(), socialThreadId);
            String oaiRunId = runService.create();
            System.out.println("Starting OpenAI social run " + oaiRunId + "...");
            runService.waitForAnswer(oaiRunId);
            
            // Get the response
            OaiMessageService msgService = new OaiMessageService(socialThreadId);
            List<String> responseMessages = msgService.listMessages();

            if (responseMessages.isEmpty()) {
                System.out.println("No messages in discussion.");
                return null;
            }
            JsonNode jsonNode = msgService.retrieveMessage(responseMessages.get(0));
            if(jsonNode==null){
                System.out.println("No messages in discussion.");
                return null;
            }
            JsonNode answerNode = jsonNode.findValue("value");
            if(answerNode==null){
                System.out.println("Answer is null");
                return null;
            }
            // Decide if the node is textual or structured
            String answer = answerNode.isTextual() 
                ? answerNode.asText()
                : objectMapper.writeValueAsString(answerNode);
            System.out.println("AI social Answer: " + answer);

            return answer;
        } catch (Exception e) {
            System.out.println("Exception in askSocialAssistantInsights: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    @PostMapping("/{did}/answer-question")
    public ResponseEntity<Message> answerQuestion(@PathVariable int did) throws IOException {
        Discussion discussion = discussionRepository.getDiscussionById(did);
        List<Message> messages = messageRepository.getAllMessagesByDiscussionId(did);
        Message lastMessage = messages.get(messages.size() - 1);
        
        Assistant assistant = assistantRepository.getAssistantByProjectId(
            discussion.projectId(), discussion.assistantType());
        Map<Types, OaiThread> threadMap = threadRepository.getAllThreadsByDiscussionId(did);
        OaiThread thread = threadMap.get(Types.all);
        OaiRunService runService = new OaiRunService(assistant.oaiAid(), thread.oaiThreadId());
        OaiMessageService msgService = new OaiMessageService(thread.oaiThreadId());
        String OaiRunId = runService.create();
        System.out.println("Starting OpenAi run " + OaiRunId + "...");
        System.out.println("Waiting for answer...");
        String socialAnswer=askSocialAssistantInsights(lastMessage.message(), discussion);
        runService.waitForAnswer(OaiRunId);
    
        JsonNode jsonNode = msgService.retrieveMessage(msgService.listMessages().get(0));
        JsonNode answerNode = jsonNode.findValue("value");
    
        // Decide if the node is textual or structured
        String answer = answerNode.isTextual() 
            ? answerNode.asText()
            : objectMapper.writeValueAsString(answerNode);
        System.out.println("AI Answer: " + answer);
        answer=answer.replaceAll("```\\w+", "").replace("```", "");
    
        // If you need to replace references:
        // Set<String> refFiles = AnswerUtils.getReferencesFileIds(answer);
        // List<OaiFile> refFileMap = oaiFileRepository.retrieveFiles(refFiles.toArray(String[]::new));
        // answer = AnswerUtils.replaceferencesFileIds(answer, refFileMap);
    
        // Validate JSON only if it’s structured
        try {
            JsonNode validated = objectMapper.readTree(answer);
            answer = objectMapper.writeValueAsString(validated);
        } catch (Exception e) {
            System.out.println("Answer is not valid JSON: " + e.getMessage());
        }
    
        // Clean up special chars
        //answer = answer.replaceAll("[\\p{Cc}&&[^\r\n\t]]", "");
    
        Message message = messageRepository.addMessage(
            new MessageCreateRequest(did, Roles.assistant.toString(), answer)
        );
        message =new Message(message,socialAnswer);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/{did}")
    public ResponseEntity<Discussion> getDiscussion(@PathVariable int did) {
        return ResponseEntity.ok(discussionRepository.getDiscussionById(did));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Discussion>> getDiscussionsByProject(@PathVariable int projectId) {
        return ResponseEntity.ok(discussionRepository.getAllDiscussionsByProjectId(projectId));
    }

    @PutMapping("/{did}")
    public ResponseEntity<Discussion> updateDiscussion(@PathVariable int did, @RequestBody DiscussionUpdateRequest updateRequest) {
        Discussion discussion=discussionRepository.updateDiscussion(updateRequest);
        return ResponseEntity.ok(discussion);
    }

    @GetMapping("{did}/suggest")
    public ResponseEntity<DiscussionNameSuggestion[]> suggestName(
        @PathVariable int did
    ) throws JsonProcessingException, IOException {
        ChatService chatService=new ChatService(
            Models.gpt_4o, 
            """
            You are a great software engineer and you are working on a project. 
            You need to suggest 5 meaningfull names and descriptions for a discussion. 
            Your answer should be formatted as a json array: [{"name": "name1", "description": "description1"}, {"name": "name2", "description": "description2"}, {"name": "name3", "description": "description3"}],
            name should be less than 25 characters long, and represent a title for the discussion, it can use up to 5 words separated by space.
            your descriptions should be less than 225 characters long.
            """,
            1f
        );
        List<Message> messages=messageRepository.getAllMessagesByDiscussionId(did);
        String json=objectMapper.writeValueAsString(messages);
        chatService.addMessage("user", json);
        String answer=chatService.answer();
        String jsonResponse=answer.substring(answer.indexOf("```json")+7, answer.lastIndexOf("```"));
        DiscussionNameSuggestion[] suggestions = objectMapper.readValue(jsonResponse, DiscussionNameSuggestion[].class);
        return ResponseEntity.ok(suggestions);
    }
    

    @DeleteMapping("/{did}")
    public ResponseEntity<Void> deleteDiscussion(@PathVariable int did) {
        discussionRepository.deleteDiscussion(did);
        return ResponseEntity.ok().build();
    }

}

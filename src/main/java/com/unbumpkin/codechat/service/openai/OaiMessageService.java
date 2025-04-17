package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import okhttp3.Request;
import okhttp3.RequestBody;


public class OaiMessageService extends BaseOpenAIClient { 

    private static final String API_URL = "https://api.openai.com/v1/threads/%s/messages";

    private final String threadId;
    public OaiMessageService(String threadId) {
        this.threadId = threadId;
    }

    public String createMessage(Roles role, String content) throws IOException {
        String url = String.format(API_URL, threadId);
        
        // Use ObjectMapper to properly escape content
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = mapper.createObjectNode();
        jsonNode.put("role", role.toString());
        jsonNode.put("content", content);
        String json = mapper.writeValueAsString(jsonNode);
        
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("OpenAI-Beta", "assistants=v2")
                .addHeader("Content-Type", "application/json")
                .build();
        return this.executeRequest(request).get("id").asText();
    }

    public List<String> listMessages() throws IOException {
        String url = String.format(API_URL, threadId);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        JsonNode jsonNode = executeRequest(request);
        List<String> messageIds = new ArrayList<>();
        for (JsonNode message : jsonNode.get("data")) {
            messageIds.add(message.get("id").asText());
        }
        return messageIds;
    }

    public JsonNode retrieveMessage(String messageId) throws IOException {
        String url = String.format(API_URL + "/%s", threadId, messageId);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();
        return this.executeRequest(request);
    }

    public JsonNode modifyMessage(String messageId, String text, String metadata) throws IOException {
        String url = String.format(API_URL + "/%s", threadId, messageId);
        Map<String,Object> modRequest = Map.of(
            "metadata", metadata
        );
        String json = objectMapper.writeValueAsString(modRequest);

        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
            .url(url)
            .put(body)
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("OpenAI-Beta", "assistants=v2")
            .addHeader("Content-Type", "application/json")
            .build();

        return this.executeRequest(request);
    }

    public void deleteMessage(String messageId) throws IOException {
        String url = String.format(API_URL + "/%s", threadId, messageId);

        Request request = new Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();

        this.executeRequest(request);
    }

}
/*
 * Curls samples of Message api
  * Create
    curl https://api.openai.com/v1/threads/thread_abc123/messages \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "OpenAI-Beta: assistants=v2" \
    -d '{
        "role": "user",
        "content": "How does AI work? Explain it in simple terms."
        }'

  * List
    curl https://api.openai.com/v1/threads/thread_abc123/messages \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "OpenAI-Beta: assistants=v2"
  * Retrieve
    curl https://api.openai.com/v1/threads/thread_abc123/messages/msg_abc123 \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "OpenAI-Beta: assistants=v2"
  * Modify
    curl https://api.openai.com/v1/threads/thread_abc123/messages/msg_abc123 \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "OpenAI-Beta: assistants=v2" \
    -d '{
        "metadata": {
            "modified": "true",
            "user": "abc123"
        }
        }'
  * delete
    curl -X DELETE https://api.openai.com/v1/threads/thread_abc123/messages/msg_abc123 \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "OpenAI-Beta: assistants=v2"
 */
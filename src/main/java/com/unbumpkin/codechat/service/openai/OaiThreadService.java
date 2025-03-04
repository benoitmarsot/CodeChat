package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Request;
import okhttp3.RequestBody;

@Service
public class OaiThreadService extends BaseOpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/threads";
    public OaiThreadService() {
        super();
    }
    public String createThread() throws IOException {
        return this.createThread(new ArrayList<>(), new HashMap<>() );
    }
    public String createThread(
        List<OaiMessageService> messages, Map<String, String> metadata
    ) throws IOException {
        String json = new ObjectMapper().writeValueAsString(new CreateThreadRequest(
            messages,metadata
        ));
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("OpenAI-Beta", "assistants=v2")
            .addHeader("Content-Type", "application/json")
            .build();
        //System.out.println("Request:"+json);

        return this.executeRequest(request).get("id").asText();
    }
    public JsonNode retrieveThread(String threadId) throws IOException {
        Request request = new Request.Builder()
                .url(API_URL+"/"+threadId)
                .get()
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();
        return this.executeRequest(request);
    }
    public JsonNode modifyThread(String threadId, Map<String, String> metadata) throws IOException {
        String json = new ObjectMapper().writeValueAsString(new CreateThreadRequest(
            null, metadata
        ));
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
            .url(API_URL+"/"+threadId)
            .post(body)
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("OpenAI-Beta", "assistants=v2")
            .addHeader("Content-Type", "application/json")
            .build();
        return this.executeRequest(request);
    }
    public void deleteThread(String threadId) throws IOException {
        Request request = new Request.Builder()
                .url(API_URL+"/"+threadId)
                .delete()
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        executeRequest(request);
    }
    
    private record CreateThreadRequest(List<OaiMessageService> messages, Map<String, String> metadata) {
    }
}
/*
 * Curls samples of Thread api
 * Create
    curl https://api.openai.com/v1/threads \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "OpenAI-Beta: assistants=v2" \
    -d ''
  * Retrieve
    curl https://api.openai.com/v1/threads/thread_abc123 \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "OpenAI-Beta: assistants=v2"
  * Modify
    curl https://api.openai.com/v1/threads/thread_abc123 \
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
    curl https://api.openai.com/v1/threads/thread_abc123 \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "OpenAI-Beta: assistants=v2" \
    -X DELETE

 */
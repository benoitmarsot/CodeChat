package com.unbumpkin.codechat.service.openai;

import java.lang.Thread;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Request;
import okhttp3.RequestBody;

public class Run extends BaseOpenAIClient { 
    private static final String API_URL = "https://api.openai.com/v1/threads/%s/runs";
    private static final String API_URL_WITH_RUN = "https://api.openai.com/v1/threads/%s/runs/%s";

    private final String assistantId;;
    private final String threadId;
    public Run(String assistantId, String threadId) {
        this.assistantId = assistantId;
        this.threadId = threadId;
    }
    public Run(String assistantId,Message.Roles role, List<Message> messages) throws IOException {
        this.assistantId = assistantId;
        this.createThreadAndRun(role, messages);
        this.threadId = "";
    }

    public String create() throws IOException {
        String url = String.format(API_URL, threadId);
        String json = String.format("{\"assistant_id\": \"%s\"}", assistantId);
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        return this.executeRequest(request).get("id").asText();
    }

    public JsonNode createThreadAndRun(Message.Roles role, List<Message> messages) throws IOException {
        String url = "https://api.openai.com/v1/threads/runs";
        String msgJson = new ObjectMapper().writeValueAsString(messages);
        String json = String.format("{\"assistant_id\": \"%s\", \"thread\": {\"messages\": %s}}, \"parallel_tool_calls\": false", assistantId, role.name(), msgJson);
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        return this.executeRequest(request);
    }

    public List<String> list() throws IOException {
        String url = String.format(API_URL, threadId);

        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();

    
        JsonNode jsonNode = executeRequest(request);
        List<String> runIds = new ArrayList<>();
        for (JsonNode run : jsonNode.get("data")) {
            runIds.add(run.get("id").asText());
        }
        return runIds;
    }

    public JsonNode retrieve( String runId) throws IOException {
        String url = String.format(API_URL_WITH_RUN, threadId, runId);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        return this.executeRequest(request);
    }

    public JsonNode modify(String runId, String metadata) throws IOException {
        String url = String.format(API_URL_WITH_RUN, threadId, runId);
        String json = String.format("{\"metadata\": %s}", metadata);
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        return this.executeRequest(request);
    }

    public JsonNode submitToolOutputs(String runId, List<ToolOutput> toolOutputs) throws IOException {
        String url = String.format(API_URL_WITH_RUN + "/submit_tool_outputs", threadId, runId);
        String json = String.format("{\"tool_outputs\": %s}", new ObjectMapper().writeValueAsString(toolOutputs));
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        return this.executeRequest(request);
    }

    public void cancelRun(String runId) throws IOException {
        String url = String.format(API_URL_WITH_RUN + "/cancel", threadId, runId);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create("", null))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        executeRequest(request);
    }
    public void waitForAnswer(String runId) {
        String status="";
        do {
            try {
                Thread.sleep((long)1000);
                JsonNode jsonNode = this.retrieve(runId);
                status=jsonNode.get("status").asText();
                JsonNode lastError=jsonNode.get("last_error");
                if(lastError.size()>0) {
                    throw new RuntimeException(lastError.get("message").textValue());
                }
                //System.out.println("Status:"+status);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        } while(!status.equals("completed")&&!status.equals("failed"));

        
    }
    private record ToolOutput(String tool_call_id, String output) {}
}
/*
 * Curls samples of Run api
  * Create
    curl https://api.openai.com/v1/threads/thread_abc123/runs \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2" \
    -d '{
        "assistant_id": "asst_abc123"
    }'
  * CreateThreadAndRun
    curl https://api.openai.com/v1/threads/runs \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2" \
    -d '{
        "assistant_id": "asst_abc123",
        "thread": {
            "messages": [
            {"role": "user", "content": "Explain deep learning to a 5 year old."}
            ]
        }
        }'

  * List
    curl https://api.openai.com/v1/threads/thread_abc123/runs \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2"
  * Retrieve
    curl https://api.openai.com/v1/threads/thread_abc123/runs/run_abc123 \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "OpenAI-Beta: assistants=v2"
  * Modify
    curl https://api.openai.com/v1/threads/thread_abc123/runs/run_abc123 \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2" \
    -d '{
        "metadata": {
        "user_id": "user_abc123"
        }
    }'
  * SubmitTool
    curl https://api.openai.com/v1/threads/thread_123/runs/run_123/submit_tool_outputs \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2" \
    -d '{
        "tool_outputs": [
        {
            "tool_call_id": "call_001",
            "output": "70 degrees and sunny."
        }
        ]
    }'
  * Cancel
    curl https://api.openai.com/v1/threads/thread_abc123/runs/run_abc123/cancel \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "OpenAI-Beta: assistants=v2" \
    -X POST
*/

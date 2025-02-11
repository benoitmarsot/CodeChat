package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.Request;

public class RunStep extends BaseOpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/threads/%s/runs/%s/steps";
    private static final String API_URL_WITH_STEP = "https://api.openai.com/v1/threads/%s/runs/%s/steps/%s";

    private final String threadId;
    private final String runId;
    public RunStep(String threadId, String runId) {
        super();
        this.threadId = threadId;
        this.runId = runId;
    }

    public List<String> listSteps() throws IOException {
        String url = String.format(API_URL, threadId, runId);

        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();

        
        JsonNode jsonNode = executeRequest(request);
        List<String> stepIds = new ArrayList<>();
        for (JsonNode step : jsonNode.get("data")) {
            stepIds.add(step.get("id").asText());
        }
        return stepIds;
    }

    public JsonNode retrieveStep(String stepId) throws IOException {
        String url = String.format(API_URL_WITH_STEP, threadId, runId, stepId);

        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();

        return this.executeRequest(request);
    }
}
/*
 * Curls samples of RunStep api
  * List
    curl https://api.openai.com/v1/threads/thread_abc123/runs/run_abc123/steps \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2"
  * Retieve
    curl https://api.openai.com/v1/threads/thread_abc123/runs/run_abc123/steps/step_abc123 \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2"

*/
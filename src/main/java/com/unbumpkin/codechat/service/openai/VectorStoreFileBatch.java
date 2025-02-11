package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.Request;
import okhttp3.RequestBody;

public class VectorStoreFileBatch extends BaseOpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/vector_stores/%s/file_batches";
    private static final String API_URL_WITH_BATCH = "https://api.openai.com/v1/vector_stores/%s/files_batches/%s";
    private static final String API_URL_WITH_BATCH_CANCEL = "https://api.openai.com/v1/vector_stores/%s/files_batches/%s/cancel";
    private static final String API_URL_WITH_BATCH_FILES = "https://api.openai.com/v1/vector_stores/%s/files_batches/%s/files";

    private final String vectorStoreId;

    public VectorStoreFileBatch(String vectorStoreId) {
        super();
        this.vectorStoreId = vectorStoreId;
    }

    public JsonNode createBatch(List<String> fileIds) throws IOException {
        String url = String.format(API_URL, vectorStoreId);

        String json = objectMapper.writeValueAsString(new CreateBatchRequest(fileIds));
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

    public JsonNode retrieveBatch(String batchId) throws IOException {
        String url = String.format(API_URL_WITH_BATCH, vectorStoreId, batchId);

        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();

        return this.executeRequest(request);
    }

    public void cancelBatch(String batchId) throws IOException {
        String url = String.format(API_URL_WITH_BATCH_CANCEL, vectorStoreId, batchId);

        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create("", null))
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();

        this.executeRequest(request);
    }

    public List<String> listBatchFiles(String batchId) throws IOException {
        String url = String.format(API_URL_WITH_BATCH_FILES, vectorStoreId, batchId);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        JsonNode jsonNode = this.executeRequest(request);
        List<String> fileIds = new ArrayList<>();
        for (JsonNode file : jsonNode.get("data")) {
            fileIds.add(file.get("id").asText());
        }
        return fileIds;
    }
    private record CreateBatchRequest(List<String> file_ids) {
    }
}
/*
 * Curls samples of VectoreStoreFileBatch api
 * Create
    curl https://api.openai.com/v1/vector_stores/vs_abc123/file_batches \
        -H "Authorization: Bearer $OPENAI_API_KEY" \
        -H "Content-Type: application/json \
        -H "OpenAI-Beta: assistants=v2" \
        -d '{
        "file_ids": ["file-abc123", "file-abc456"]
        }'
 * Retrieve
    curl https://api.openai.com/v1/vector_stores/vs_abc123/files_batches/vsfb_abc123 \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2"
 * CancelBatch
    curl https://api.openai.com/v1/vector_stores/vs_abc123/files_batches/vsfb_abc123/cancel \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2" \
    -X POST
 * List
    curl https://api.openai.com/v1/vector_stores/vs_abc123/files_batches/vsfb_abc123/files \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2"

 */

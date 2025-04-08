package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.dto.request.CreateVSFileRequest;
import com.unbumpkin.codechat.util.JsonUtils;

import okhttp3.Request;
import okhttp3.RequestBody;

public class VectorStoreFile extends BaseOpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/vector_stores/%s/files";
    private static final String API_URL_WITH_FILE = "https://api.openai.com/v1/vector_stores/%s/files/%s";

    private final String vectorStoreId;
    public VectorStoreFile(String vectorStoreId) {
        super();
        this.vectorStoreId = vectorStoreId;
    }

    public String addFile( CreateVSFileRequest createVSFileRequest) throws IOException {
        String url = String.format(API_URL, vectorStoreId);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        
        String json = mapper.writeValueAsString(createVSFileRequest);
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();
        JsonNode jsonNode = executeRequest(request);
        String error = JsonUtils.getOpenAiError(jsonNode);
        if (error != null) {
            throw new IOException("Error from OpenAI API: " + error);
        }
        return jsonNode.get("id").asText();
    }

    public List<String> listFiles() throws IOException {
        String url = String.format(API_URL, vectorStoreId);

        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();

        JsonNode jsonNode = executeRequest(request);
        List<String> fileIds = new ArrayList<>();
        for (JsonNode file : jsonNode.get("data")) {
            fileIds.add(file.get("id").asText());
        }
        return fileIds;
    }

    public JsonNode retrieveFile( String fileId) throws IOException {
        String url = String.format(API_URL_WITH_FILE, vectorStoreId, fileId);

        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();

        return executeRequest(request);
    }

    public void removeFile( String fileId) throws IOException {
        String url = String.format(API_URL_WITH_FILE, vectorStoreId, fileId);

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
 * Curls samples of VectoreStoreFile api
  * Create
    curl https://api.openai.com/v1/vector_stores/vs_mUs6SCs4dCxSjKH9Fi4Np3vW/files \
        -H "Authorization: Bearer $OPENAI_API_KEY" \
        -H "Content-Type: application/json" \
        -H "OpenAI-Beta: assistants=v2" \
        -d '{
        "file_id": "file-QtPMcPW5SQ8TSYxTEb4ug5"
        }'
  * List
    curl https://api.openai.com/v1/vector_stores/vs_abc123/files \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2"
  * Retrieve
    curl https://api.openai.com/v1/vector_stores/vs_abc123/files/file-abc123 \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2"
  * delete
    curl https://api.openai.com/v1/vector_stores/vs_abc123/files/file-abc123 \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2" \
    -X DELETE
*/
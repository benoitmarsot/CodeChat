package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.Request;
import okhttp3.RequestBody;

@Service
public class OaiEmbeddingService extends BaseOpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/embeddings";

    public float[] getOpenAIEmbedding(String chunk) throws Exception {
        return this.getOpenAIEmbedding(List.of(chunk)).get(0);
    }

    /**
     * Get OpenAI embedding for a list of chunks.
     * Respect the OpenAI API token limit.
     * The OpenAI API has a limit of 8192 tokens for the text-embedding-ada-002 model.
     * @param chunks List of strings to be embedded.
     * @return Array of floats representing the embedding.
     * @throws Exception if an error occurs during the request.
     */
    public List<float[]> getOpenAIEmbedding(List<String> chunks) throws Exception {
        Map<String,Object> embedRequest = Map.of(
            "input", chunks,
            "model", "text-embedding-ada-002",
            "encoding_format", "float"
        );
        String json = objectMapper.writeValueAsString(embedRequest);
    
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
            .url(API_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + API_KEY)
            .post(body)
            .build();

        JsonNode jsonResponse = this.executeRequest(request);
        if (jsonResponse == null) {
            throw new IOException("Failed to create run: No response from server");
        }
        if (jsonResponse.has("error")) {
            throw new IOException("Failed to create run: " + jsonResponse.get("error").asText());
        }
        // Parse embeddings from the "data" array
        JsonNode dataArray = jsonResponse.get("data");
        if (dataArray == null || !dataArray.isArray()) {
            throw new IOException("No 'data' field in OpenAI response");
        }
        List<float[]> embeddings = new java.util.ArrayList<>();
        for (JsonNode item : dataArray) {
            JsonNode embeddingNode = item.get("embedding");
            if (embeddingNode == null || !embeddingNode.isArray()) {
                throw new IOException("No 'embedding' field in OpenAI response item");
            }
            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }
            embeddings.add(embedding);
        }
        return embeddings;
    }
}

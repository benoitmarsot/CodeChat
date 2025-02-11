package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.Request;
import okhttp3.RequestBody;

@Service
public class VectorStore extends BaseOpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/vector_stores";
    private static final String API_URL_WITH_ID = "https://api.openai.com/v1/vector_stores/%s";

    public VectorStore() {
        super();
    }
    public String createVectorStore(String name) throws IOException {
        String json = String.format("{\"name\": \"%s\"}", name);
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();

        return this.executeRequest(request).get("id").asText();
    }
    //Warning: this method will delete all vector stores
    public void cleanUpVectorStores() {
        try {
            List<String> vectorStoreIds=this.listVectorStores();
            System.out.println("there is "+vectorStoreIds.size()+" vector stores, deleting them:");
            vectorStoreIds.forEach(System.out::println);
            vectorStoreIds.forEach( vs-> {
                try {
                    this.deleteVectorStore(vs);
                    System.out.println("Deleted vector store "+vs+ "...");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            vectorStoreIds=this.listVectorStores();
            System.out.println("there is "+vectorStoreIds.size()+" vector store left.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public List<String> listVectorStores() throws IOException {
        Request request = new Request.Builder()
                .url(API_URL)
                .get()
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        JsonNode jsonNode = executeRequest(request);
        List<String> vectorStoreIds = new ArrayList<>();
        for (JsonNode vectorStore : jsonNode.get("data")) {
            vectorStoreIds.add(vectorStore.get("id").asText());
        }
        return vectorStoreIds;
    }

    public JsonNode retrieveVectorStore(String vectorStoreId) throws IOException {
        String url = String.format(API_URL_WITH_ID, vectorStoreId);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        return this.executeRequest(request);
    }

    public JsonNode modifyVectorStore(String vectorStoreId, String name) throws IOException {
        String url = String.format(API_URL_WITH_ID, vectorStoreId);
        String json = String.format("{\"name\": \"%s\"}", name);
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

    public void deleteVectorStore(String vectorStoreId) throws IOException {
        String url = String.format(API_URL_WITH_ID, vectorStoreId);

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        executeRequest(request);
    }

}
/*
 * Curls samples of Vector api
  * Create
    curl https://api.openai.com/v1/vector_stores \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2"
    -d '{
        "name": "Support FAQ"
    }'
  * List
    curl https://api.openai.com/v1/vector_stores \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2"
  * Retrieve
    curl https://api.openai.com/v1/vector_stores/vs_abc123 \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2"
  * Modify
    curl https://api.openai.com/v1/vector_stores/vs_abc123 \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2"
    -d '{
        "name": "Support FAQ"
    }'
  * delete
    curl https://api.openai.com/v1/vector_stores/vs_abc123 \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -H "OpenAI-Beta: assistants=v2" \
    -X DELETE
 */

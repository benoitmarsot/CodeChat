package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.unbumpkin.codechat.model.openai.VectorStore;

import okhttp3.Request;
import okhttp3.RequestBody;

@Service
public class VectorStoreService extends BaseOpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/vector_stores";
    private static final String API_URL_WITH_ID = "https://api.openai.com/v1/vector_stores/%s";


    public VectorStoreService() {
        super();
    }

    /**
     * Creates a vector store using the OpenAI API.
     *
     * @param vectorStore The vector store to create.
     * @return The ID of the newly created vector store.
     * @throws IOException if an I/O error occurs.
     */
    public String createVectorStore(
        VectorStore vectorStore
    ) throws IOException {
        if(vectorStore.getFileIds().size()>100) {
            System.out.println("Creating vector store with more than 100 files...");
            List<String> allFileIds=vectorStore.getFileIds();
            System.out.println("Total files: "+allFileIds.size());
            //Get the first 100 files
            List<String> fileIds = new ArrayList<>(vectorStore.getFileIds().subList(0, 100));
            vectorStore.setFileIds(fileIds);
            System.out.println("Creating vector store with 100 files...");
            String vsId=this.createVectorStore(vectorStore);
            VectorStoreFileBatch vectorStoreFileBatch = new VectorStoreFileBatch(vsId);
            //Get the rest of the files in chunk of 100
            for(int i=100; i<allFileIds.size(); i+=100) {
                fileIds = new ArrayList<>(allFileIds.subList(i, Math.min(i+100, allFileIds.size())));
                vectorStore.setFileIds(fileIds);
                System.out.println("Submitting "+fileIds.size()+" files batch to "+vsId+"...");
                vectorStoreFileBatch.createBatch(fileIds);
            }
            return vsId;
        }
        RequestBody body = RequestBody.create(getCreateRequest(vectorStore), JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .build();

        JsonNode response = this.executeRequest(request);
        return response.get("id").asText();
    }
 
    //Warning: this method will delete all vector stores
    /**
     * Deletes all vector stores.
     */
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
    /**
     * Lists all vector stores.
     * @return
     * @throws IOException
     */
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

    public JsonNode modifyVectorStore(VectorStore vectorStore) throws IOException {
        String url = String.format(API_URL_WITH_ID, vectorStore.getVsid());
        RequestBody body = RequestBody.create(getServiceModifyRequest(vectorStore), JSON_MEDIA_TYPE);

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
    private String getCreateRequest(VectorStore vectoreStore) throws IOException {        
        Map<String, Object> requestBody = new LinkedHashMap<>();
        if (vectoreStore.getVsname() != null && !vectoreStore.getVsname().isEmpty()) {
            requestBody.put("name", vectoreStore.getVsname());
        }
        if (vectoreStore.getFileIds() != null && !vectoreStore.getFileIds().isEmpty()) {
            requestBody.put("file_ids", vectoreStore.getFileIds());
        }
        if (vectoreStore.getExpiresAfter() != null && !vectoreStore.getExpiresAfter().isEmpty()) {
            requestBody.put("expires_after", vectoreStore.getExpiresAfter());
        }
        if (vectoreStore.getChunkingStrategy() != null && !vectoreStore.getChunkingStrategy().isEmpty()) {
            requestBody.put("chunking_strategy", vectoreStore.getChunkingStrategy());
        }
        if (vectoreStore.getMetadata() != null && !vectoreStore.getMetadata().isEmpty()) {
            requestBody.put("metadata", vectoreStore.getMetadata());
        }
        return objectMapper.writeValueAsString(requestBody);
    }
    public String getServiceModifyRequest(VectorStore vectorStore) throws JsonProcessingException {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        if (vectorStore.getVsname() != null && !vectorStore.getVsname().isEmpty()) {
            requestBody.put("name", vectorStore.getVsname());
        }
        if (vectorStore.getExpiresAfter() != null && !vectorStore.getExpiresAfter().isEmpty()) {
            requestBody.put("expires_after", vectorStore.getExpiresAfter());
        }
        if (vectorStore.getMetadata() != null && !vectorStore.getMetadata().isEmpty()) {
            requestBody.put("metadata", vectorStore.getMetadata());
        }
        return objectMapper.writeValueAsString(requestBody);
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

package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class BaseOpenAIClient {
    public enum Models {
        gpt_4o("gpt-4o"),
        gpt_4o_mini("gpt-4o-mini"),
        gpt_3_5_turbo("gpt-3.5-turbo"),
        gpt_4("gpt-4"),
        gpt_4_5_preview("gpt-4.5-preview"),
        gpt_4_vision_preview("gpt-4-vision-preview"),
        gpt_4_turbo("gpt-4-turbo"),
        gpt_4o_realtime_preview("gpt-4o-realtime-preview"),
        o3_mini("o3-mini"),
        o1("o1"),
        o1_pro("o1-pro"); //Carefull very $$$

        private String model;

        Models(String model) {
            this.model = model;
        }
        public static Models fromString(String model) {
            for (Models m : Models.values()) {
                if (m.model.equals(model)) {
                    return m;
                }
            }
            return null;
        }
        @Override
        public String toString() {
            return model;
        }
    }
    public enum Roles {
        system,
        user,
        assistant
    }

    protected static final String API_KEY = System.getenv("OPENAI_API_KEY");
    protected static MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    protected OkHttpClient client;
    protected ObjectMapper objectMapper;

    public BaseOpenAIClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.setSerializationInclusion(Include.NON_NULL);

    }
    protected JsonNode executeRequest(Request request) throws IOException {
        int maxRetries = 3;
        int retryCount = 0;
        IOException lastException = null;
    
        while (retryCount < maxRetries) {
            try {
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = new String(response.body().bytes(), StandardCharsets.UTF_8);
                    if (!response.isSuccessful() && response.code() == 502) {
                        throw new SocketTimeoutException(responseBody);
                    }
                    return objectMapper.readTree(responseBody);
                }
            } catch (SocketTimeoutException e) {
                lastException = e;
                retryCount++;
                
                if (retryCount >= maxRetries) {
                    break;
                }
                
                try {
                    Thread.sleep(1000 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted during retry", ie);
                }
                
                System.out.println("Retrying request, attempt " + (retryCount + 1) + " of " + maxRetries);
            }
        }
        
        // If we've exhausted all retries, throw the last exception
        throw new IOException("Request failed after " + maxRetries + " attempts: " + (lastException==null?"":lastException.getMessage()), lastException);
    }
    
}
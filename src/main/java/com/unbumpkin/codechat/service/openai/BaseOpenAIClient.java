package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
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
        gpt_4_turbo("gpt-4-turbo"),
        gpt_4o_realtime_preview("gpt-4o-realtime-preview"),
        o3_mini("o3-mini");
        private String model;

        Models(String model) {
            this.model = model;
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
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.setSerializationInclusion(Include.NON_NULL);

    }
    protected JsonNode executeRequest(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException(responseBody);
            }
            return objectMapper.readTree(responseBody);
        }
    }
    
}
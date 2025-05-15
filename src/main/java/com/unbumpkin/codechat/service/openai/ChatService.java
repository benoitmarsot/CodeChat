/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Request;
import okhttp3.RequestBody;


/**
 *
 * @author benoitmarsot
 */
public class ChatService extends BaseOpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private final Models model;
    private final ChatMessage instruction;
    private final List<ChatMessage> chatMessages;
    private float temperature;
    public ChatService(Models model, String systemInstruction, float temperature) {
        this.model = model;
        this.instruction=new ChatMessage("system", systemInstruction);
        this.chatMessages=new ArrayList<>();
        this.chatMessages.add(this.instruction);
        this.temperature=temperature;
    }

    public String answer() throws IOException {
        String json = new ObjectMapper().writeValueAsString(
            new ChatRequest(this.model.toString(), chatMessages, this.temperature)
        );
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        JsonNode response=this.executeRequest(request);
        if (response.get("error") != null) {
                throw new IOException("Error: " + response.get("error").get("message").asText());
        }

        JsonNode choices=response.get("choices");
        if (!choices.isEmpty()) {
            return choices.get(0).get("message").get("content").asText();
        }
        return response.get("Body").asText();
        
    }
    public void addMessage(ChatMessage message) {
        this.chatMessages.add(message);
    }
    public void addMessage(String role, String content) {
        this.chatMessages.add(new ChatMessage(role, content));
    }

    private record ChatRequest(String model, List<ChatMessage> messages, double temperature) {
    }
    private record ChatMessage(String role, String content) {
    }

}

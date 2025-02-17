/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Request;
import okhttp3.RequestBody;


/**
 *
 * @author benoitmarsot
 */
public class Chat extends BaseOpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private final Models model;
    private List<Message> messages;
    public Chat(Models model) {
        this.messages = new ArrayList<>();
        this.model = model;
        messages.add(new Message("system", "You are a helpful assistant."));
    }

    public void start() {
        System.out.println("Chat with "+model.toString()+"!");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter your prompt (type 'bye' to quit): ");
            String userPrompt = scanner.nextLine();
            if (userPrompt.equalsIgnoreCase("bye")) {
                break;
            }
            messages.add(new Message("user", userPrompt));
            try {
                JsonNode jsonNode = generateText(userPrompt);
                String textResponse=jsonNode.get("choices").get(0).get("message").get("content").asText();
                messages.add(new Message("assistant", textResponse));
                System.out.println(
                    String.format("Response from OpenAI: %s",
                        textResponse
                ));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        scanner.close();
    }
    private JsonNode generateText(String prompt) throws IOException {

        String json = new ObjectMapper().writeValueAsString(
            new ChatRequest(this.model.toString(), messages, 0.5)
        );
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();


        return this.executeRequest(request);
    }
    private record ChatRequest(String model, List<Message> messages, double temperature) {
    }
    private record Message(String role, String content) {
    }

}

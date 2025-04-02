package com.unbumpkin.codechat.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonUtils {
    public static String loadJson(String fileName) throws IOException {
        return new String(
            Files.readAllBytes(
                Paths.get(
                    Objects.requireNonNull(
                        JsonUtils.class.getClassLoader().getResource(fileName)
                    ).getPath())),
            StandardCharsets.UTF_8
        );
    }
    public static String getOpenAiError(JsonNode answer) {
        if (answer.has("error") && !answer.get("error").toString().equals("null")) {
            JsonNode error = answer.get("error");
            String errorMessage = error.has("message") ? 
                error.get("message").asText() : "Unknown error from OpenAI API";
            String errorType = error.has("type") ?
                error.get("type").asText() : "api_error";
            return "OpenAI API error (" + errorType + "): " + errorMessage;
        }
        return null;
    }
}
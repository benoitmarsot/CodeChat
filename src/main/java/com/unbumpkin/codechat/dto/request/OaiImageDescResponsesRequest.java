package com.unbumpkin.codechat.dto.request;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Roles;

public record OaiImageDescResponsesRequest(
    Models model, String instruction, String encodedImage, Details detail
) {
    public enum Details {
        low, high, auto
    }
    // public String getBase64EncodedImage() {
    //     return "data:image/png;base64," + new String(encodedImage);
    // }
    public String toJsonString(
        String responseTemplate
    ) throws JsonProcessingException {
        Map<String, Object> requestMap=Map.of(
            "model", model.toString(),
            "input", List.of(
                Map.of(
                    "role", Roles.user,
                    "content", List.of(
                        // Map.of(
                        //     "type", "input_text",
                        //     "text", instruction
                        // ),
                        Map.of(
                            "type", "input_image",
                            "image_url", encodedImage,
                            "detail", detail
                        )
                    )
                )
            ),"text","{responseFormat}"
        );
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonStr=objectMapper.writeValueAsString(requestMap);
        return jsonStr.replace("\"{responseFormat}\"", responseTemplate);
    }
    public String toJsonString() throws JsonProcessingException {
        Map<String, Object> requestMap=Map.of(
            "model", model.toString(),
            "input", List.of(
                Map.of(
                    "role", Roles.system,
                    "content", List.of(
                        Map.of(
                            "type", "input_text",
                            "text", instruction
                        )
                    )
                ),
                Map.of(
                    "role", Roles.user,
                    "content", List.of(
                        Map.of(
                            "type", "input_image",
                            "image_url", encodedImage,
                            "detail", detail
                        )
                    )
                )
            ),"text",Map.of(
                "format", Map.of(
                    "type", "text"
                )
            ),
            "reasoning", Map.of(),
            "tools", List.of(),
            "temperature", 1,
            "max_output_tokens", 10000,
            "top_p", 1,
            "store", false
        );
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonStr=objectMapper.writeValueAsString(requestMap);
        return jsonStr;
    }
}


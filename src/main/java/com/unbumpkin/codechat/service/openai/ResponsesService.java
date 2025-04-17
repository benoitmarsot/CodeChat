package com.unbumpkin.codechat.service.openai;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.unbumpkin.codechat.dto.request.OaiImageDescResponsesRequest;
import com.unbumpkin.codechat.util.JsonUtils;

import okhttp3.Request;
import okhttp3.RequestBody;

@Service
public class ResponsesService extends BaseOpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/responses";

    public ResponsesService() {
        super();
    }

    public String describeImage(
        OaiImageDescResponsesRequest imageRequest, boolean noJson_schema
    ) throws IOException {
        String json;
        if( noJson_schema ) {
            json = imageRequest.toJsonString();
        } else {
            String responseTemplate=JsonUtils.loadJson( "json_schema/fast_image_descriptionresponse.json");
            json = imageRequest.toJsonString(responseTemplate);
        }
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();
        JsonNode answer=this.executeRequest(request);

        String error=JsonUtils.getOpenAiError(answer);
        if (error != null) {
            throw new IOException("Error from OpenAI API: " + error);
        }
        // Find first output node with content
        String result = null;
        if (answer.has("output") && answer.get("output").isArray()) {
            JsonNode outputArray = answer.get("output");
            for (JsonNode output : outputArray) {
                if (output.has("content") && output.get("content").isArray()) {
                    JsonNode contentArray = output.get("content");
                    for (JsonNode content : contentArray) {
                        if (content.has("text")) {
                            result = content.get("text").asText();
                            break;
                        }
                    }
                    if (result != null) {
                        break;
                    }
                }
            }
        }
        
        if (result == null) {
            throw new IOException("No valid text content found in response: " + answer.toString());
        }
        
        return result;
    }

}

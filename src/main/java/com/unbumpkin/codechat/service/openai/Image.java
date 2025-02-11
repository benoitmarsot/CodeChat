package com.unbumpkin.codechat.service.openai;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

@Service
public class Image extends BaseOpenAIClient {  
    private static final String API_URL_GENERATIONS = "https://api.openai.com/v1/images/generations";
    private static final String API_URL_EDITS = "https://api.openai.com/v1/images/edits";
    private static final String API_URL_VARIATIONS = "https://api.openai.com/v1/images/variations";

    public Image() {
        super();
    }

    public String createImage(String model, String prompt, int n, String size) throws IOException {
        String url = API_URL_GENERATIONS;

            String json = new ObjectMapper().writeValueAsString(
            new CreateImageRequest(model, prompt, n, size)
        );

        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();
        return this.executeRequest(request).get("id").asText();
    }

    public JsonNode editImage(String imagePath, String maskPath, String prompt, int n, String size) throws IOException {
        String url = API_URL_EDITS;

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", imagePath, RequestBody.create(new java.io.File(imagePath), MediaType.parse("image/png")))
                .addFormDataPart("mask", maskPath, RequestBody.create(new java.io.File(maskPath), MediaType.parse("image/png")))
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("n", String.valueOf(n))
                .addFormDataPart("size", size)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        return this.executeRequest(request);    
    }

    public JsonNode createVariation(String imagePath, int n, String size) throws IOException {
        String url = API_URL_VARIATIONS;

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", imagePath, RequestBody.create(new java.io.File(imagePath), MediaType.parse("image/png")))
                .addFormDataPart("n", String.valueOf(n))
                .addFormDataPart("size", size)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        return this.executeRequest(request);
    }
    private record CreateImageRequest(String model, String prompt, int n, String size) {
    }
}

/*
 * Curls samples of Image api
 * Create
    curl https://api.openai.com/v1/images/generations \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $OPENAI_API_KEY" \
      -d '{
        "model": "dall-e-3",
        "prompt": "A cute baby sea otter",
        "n": 1,
        "size": "1024x1024"
      }'
  * Edit
    curl https://api.openai.com/v1/images/edits \
      -H "Authorization: Bearer $OPENAI_API_KEY" \
      -F image="@otter.png" \
      -F mask="@mask.png" \
      -F prompt="A cute baby sea otter wearing a beret" \
      -F n=2 \
      -F size="1024x1024"
  * CreateVariation
     curl https://api.openai.com/v1/images/variations \
      -H "Authorization: Bearer $OPENAI_API_KEY" \
      -F image="@otter.png" \
      -F n=2 \
      -F size="1024x1024"
*/

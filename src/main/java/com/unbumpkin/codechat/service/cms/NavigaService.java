package com.unbumpkin.codechat.service.cms;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.unbumpkin.codechat.dto.cms.Article;

@Service
public class NavigaService {
    protected static final String DP_NAVIGA_UNAME = System.getenv("DP_NAVIGA_UNAME");
    protected static final String DP_NAVIGA_PW = System.getenv("DP_NAVIGA_PW");
    protected static MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    protected OkHttpClient client;
    protected static final String BASE_URL = "https://xlibris.public.prod.oc.srp.navigacloud.com:8443";

    @Autowired
    protected ObjectMapper mapper;

    public NavigaService() {
        this.client = new OkHttpClient();
    }

    /**
     * Performs a search against Naviga Cloud.
     *
     * @param query the search query
     * @param start pagination start index
     * @param limit max number of results
     * @return raw JSON response as String
     * @throws IOException on network / I/O errors
     */
    public List<Article> search(String query, int start, int limit) throws IOException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String url = String.format(
            "%s/opencontent/search?start=%d&limit=%d&deleted=false&q=%s",
            BASE_URL, start, limit, encodedQuery
        );

        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("accept", "*/*")
            .addHeader("Authorization", Credentials.basic(DP_NAVIGA_UNAME, DP_NAVIGA_PW))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected HTTP code " + response.code() + ": " + response.message());
            }
            return response.body() != null ? parseArticles(response.body().string()) : null;
        }
    }
    private List<String> getNodeList(JsonNode props, String fieldName) {
        List<String> list = new ArrayList<>();
        JsonNode node = props.path(fieldName);
        if (node.isArray()) {
            for (JsonNode item : node) {
                list.add(item.asText());
            }
        }
        return list;
    }
    /**
     * Parse the full response JSON and extract a list of Articles.
     */
    public List<Article> parseArticles(String responseJson) throws IOException {
        JsonNode root = mapper.readTree(responseJson);
        JsonNode hits = root.path("hits").path("hits");
        List<Article> list = new ArrayList<>();

        for (JsonNode hit : hits) {
            JsonNode props = hit.path("versions").get(0).path("properties");

            // title
            List<String> titles = getNodeList(props, "WriterHeadlines");
            if(titles.isEmpty()) {
                titles=getNodeList(props, "TeaserHeadline");
            }
            String title=titles.isEmpty() ? "" : titles.get(0);
            
            // url
            List<String> primary = getNodeList(props, "primary");
            // publication date
            List<String> publicationdates = getNodeList(props,"Pubdate");
            String publicationdate = publicationdates.isEmpty() ? "" : publicationdates.get(0);
            // author
            List<String> authors = getNodeList(props,"Byline");
            authors = authors.isEmpty()?getNodeList(props,"BylineAttribution"):authors;
            // images
            List<String> images = getNodeList(props, "ArticleMetaImageUuids");
            // original URLs
            List<String> originalUrls = getNodeList(props, "OriginalUrl");
            // teaser body
            List<String> teaserBodies= getNodeList(props,"TeaserBody");
            String teaserBody = teaserBodies.isEmpty() ? "" : teaserBodies.get(0);
            // body
            List<String> bodiesRaw=getNodeList(props,"Text");
            String bodyRaw = bodiesRaw.isEmpty() ? "" : bodiesRaw.get(0);
            bodyRaw = bodyRaw.replaceAll("<[^>]+>", "").trim();
            List<String> names = getNodeList(props, "Name");
            String name = names.isEmpty() ? "" : names.get(0);

            // create article
            Article a = new Article(title, teaserBody, bodyRaw, name, primary, originalUrls, publicationdate, authors, images);
            // add to list
            list.add(a);
        }

        return list;
    }
}
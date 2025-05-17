package com.unbumpkin.codechat.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.dto.cms.SearchedArticle;
import com.unbumpkin.codechat.dto.cms.ArticleReferences;
import com.unbumpkin.codechat.dto.cms.PDSearchRequest;
import com.unbumpkin.codechat.service.cms.NavigaService;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;
import com.unbumpkin.codechat.service.openai.ChatService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/pd")
public class PDController {
    @Autowired
    private NavigaService navigaService;
    @Autowired
    ObjectMapper objectMapper;

    /**
     * Search for articles in Naviga Cloud.
     * @param request the search request
     * @return a list of articles matching the search criteria
     * @throws IOException on network / I/O errors
     */
    @PostMapping("/search")
    public ResponseEntity<List<SearchedArticle>> search(
        @RequestBody PDSearchRequest request
    ) throws IOException {
        List<SearchedArticle> results=navigaService.search( request.query(), request.start(), request.limit());
        
        return ResponseEntity.ok(results);
    }
    /**
     * Ask the social assistant for insights on result produced by naviga 
     * elastic searh + Ai.
     * @param request the search request
     * @return ArticlelReferences object with the references and overall description
     */
    @PostMapping("/ai-search")
    public ResponseEntity<ArticleReferences> aiSearch(
        @RequestBody PDSearchRequest request
    ) throws IOException {
        List<SearchedArticle> results=navigaService.search( request.query(), request.start(), request.limit());
        // ObjectMapper mapper=new ObjectMapper();
        // mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // System.out.println("Naviga results: \n" + mapper.writeValueAsString(results));
        ArticleReferences references=getAiReferences(request.query(), results);
        
        return ResponseEntity.ok(references);
    }

    /**
     * Ask the social assistant for insights on result produced by our pgVector
     * store+Ai if availabe or Ai SocialVectore if available.
     * @param query the question asked
     * @param articles the list of articles return as an answer from elasticsearch
     * @return ArticlelReferences object with the references and overall description
     */
    private ArticleReferences getAiReferences(
        String query, List<SearchedArticle> articles
    ) throws IOException {
        ChatService chatService=new ChatService(
            // Maybe we should look at other vendors? Gemini? Anthropic?, 
            // Models.gpt_4o_realtime
            // Do not get all authors, but faster, 11-24 seconds, 
            Models.gpt_4_1_nano
            // Seem to get all the authors but is 2x slower, so?, 30-40 seconds
            // Models.gpt_4_turbo // $$$
            // very good output but slow 60-120 seconds
            // Models.o4_mini 
            , """
            Answer the 'query' in detail the 'body' of all the articles. 
            Provide a brief overall description summarizing the articles that answer the query. 
            Then list metadata for each article. Your response must be a well-formatted JSON object with the following structure:

            {
            "answer": "<answer to the query>",
            "overallDescription": "<brief overall summary>",
            "articles": [
                {
                "publicationDate": "YYYY-MM-DDTHH:MM:SSZ",  // ISO 8601 UTC timestamp
                "authors": ["author1", "author2", ...],
                "name": "Short title (max 25 chars, max 7 words)",
                "description": "Headline-style summary (max 225 characters)",
                "primary": ["primary1", "primary2", ...],
                "originalUrls": ["https://originalUrl1", "https://originalUrl2", ...],
                "images": ["image1", "image2", ...]
                },
                ...
            ]
            }

            Formatting rules:
            - If 'title' is available, use it for 'description'. Otherwise, create a headline-style summary from 'teaserBody' or the article 'body'.
            - If the author is not directly provided, extract it from the article body or contact section using terms such as by, written by, columnist, journalist, reporter, editor, writer, staff writer, or by detecting email addresses, phone numbers, or named social handles associated with a person. When an author name is found in a phrase such as “Contact Columnist name…” or ”…reach name at…”, use the name as the author.
            - 'name' must be created from 'teaserBody' or 'body', and must be no longer than 25 characters or 7 words.
            - All strings must be clean of HTML and properly quoted.

            Ensure all values conform to the types and constraints above.
            """, 1f
        );
        Map<String, Object> articleMap=new HashMap<>();
        articleMap.put("query", query);
        articleMap.put("articles", articles);
        String messageTxt=objectMapper.writeValueAsString(articleMap);
        chatService.addMessage("user", messageTxt);
        String answer=chatService.answer();
        if(answer.indexOf("```json")>-1) {
            answer=answer.substring(answer.indexOf("```json")+7, answer.lastIndexOf("```"));
        }
        System.out.println("AI Answer: " + answer);
        ArticleReferences references = objectMapper.readValue(answer, ArticleReferences.class);
        return references;
    }
    
}

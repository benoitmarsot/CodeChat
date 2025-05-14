package com.unbumpkin.codechat.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.dto.cms.SearchedArticle;
import com.unbumpkin.codechat.dto.cms.AiRefArticle;
import com.unbumpkin.codechat.dto.cms.ArticleReferences;
import com.unbumpkin.codechat.dto.cms.PDSearchRequest;
import com.unbumpkin.codechat.dto.social.SocialReferences;
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

    @PostMapping("/search")
    public ResponseEntity<List<SearchedArticle>> search(
        @RequestBody PDSearchRequest request
    ) throws IOException {
        List<SearchedArticle> results=navigaService.search( request.query(), request.start(), request.limit());
        
        return ResponseEntity.ok(results);
    }
    @PostMapping("/ai-search")
    public ResponseEntity<ArticleReferences> aiSearch(
        @RequestBody PDSearchRequest request
    ) throws IOException {
        List<SearchedArticle> results=navigaService.search( request.query(), request.start(), request.limit());
        ArticleReferences references=getAiReferences(request.query(), results);
        
        return ResponseEntity.ok(references);
    }

    /**
     * Ask the social assistant for insights on result produced by our pgVector
     * store+Ai if availabe or Ai SocialVectore if available.
     * @param query the question asked
     * @param articles the list of articles return as an answer from elasticsearch
     * @return SocialReferences object with the references and overall description
     * @throws Exception
     */
    private ArticleReferences getAiReferences(
        String query, List<SearchedArticle> articles
    ) throws IOException {
        ChatService chatService=new ChatService(
            Models.gpt_4_1_nano, """
            Give an overall bref description of all the articles answering the query, and then use the authors and a few word description for each. 
            Your answer should be formatted as a json object: 
            {
                "overallDescription":<overall bref description>,
                "articles": [
                    {"publicationDate":"publicationdate1","author": "author1", "name": "name1", "description": "description1", "primary"=["primary1","primary2",..], "urls"=["originalUrls1",""originalUrls2",...], "images"=["image1","image2",...]}, 
                    {"publicationDate":"publicationdate1","author": "author1", "name": "name1", "description": "description1", "primary"=["primary1","primary2",..], "urls"=["originalUrls1",""originalUrls2",...], "images"=["image1","image2",...]},
                    {"publicationDate":"publicationdate3","author": "author3", "name": "name3", "description": "description3", "primary"=["primary1","primary2",..], "urls"=["originalUrls1",""originalUrls2",...], "images"=["image1","image2",...]},
                    ...
                ]
            },
            name should be less than 25 characters long, and represent a title for the message, 
            it can use up to 7 words separated by space. It is created using the teaserBody or the body of the article.

            If authors are not present, try to find the authors in the body of the article,
            
            your descriptions should be less than 225 characters long. it is the title property if present, if not present use the teaserBody and body of the article to create an headline style title.
            """,
            1f
        );
        Map<String, Object> messageMap=new HashMap<>();
        messageMap.put("query", query);
        messageMap.put("articles", articles);
        String messageTxt=objectMapper.writeValueAsString(messageMap);
        chatService.addMessage("user", messageTxt);
        String answer=chatService.answer();
        if(answer.indexOf("```json")>-1) {
            answer=answer.substring(answer.indexOf("```json")+7, answer.lastIndexOf("```"));
        }
        System.out.println("AI social Answer: " + answer);
        ArticleReferences references = objectMapper.readValue(answer, ArticleReferences.class);
        return references;
    }
    
}

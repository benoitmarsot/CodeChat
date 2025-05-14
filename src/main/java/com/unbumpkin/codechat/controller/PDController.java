package com.unbumpkin.codechat.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unbumpkin.codechat.dto.cms.Article;
import com.unbumpkin.codechat.dto.cms.PDSearchRequest;
import com.unbumpkin.codechat.service.cms.NavigaService;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/pd")
public class PDController {
    @Autowired
    private NavigaService navigaService;

    @PostMapping("search")
    public ResponseEntity<List<Article>> postMethodName(
        @RequestBody PDSearchRequest request
    ) throws IOException {
        List<Article> results=navigaService.search( request.query(), request.start(), request.limit());
        
        return ResponseEntity.ok(results);
    }
    
}

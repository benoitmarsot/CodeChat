package com.unbumpkin.codechat.controller.openai;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.repository.openai.OaiFileRepository;
import com.unbumpkin.codechat.service.openai.OaiFileService;
import com.unbumpkin.codechat.service.openai.ProjectFileCategorizer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/openai/assistant")
@Tag(name = "OpenAI Assistant Controller", description = "Endpoints for managing OpenAI assistants")
public class AssistantController {
    @Autowired
    private final OaiFileService oaiFileService;

    @Autowired
    private final OaiFileRepository oaiFileRepository;
    
    
    public AssistantController(OaiFileService oaiFileService, OaiFileRepository oaiFileRepository, ObjectMapper objectMapper) {
        this.oaiFileService = oaiFileService;
        this.oaiFileRepository = oaiFileRepository;
    }

    @Operation(summary = "Create a new assistant for programming")
    @PostMapping
    public void createAssistant(
        @RequestBody String rootDir
    ) throws IOException {
        // Create a new assistant
        ProjectFileCategorizer pvs=new ProjectFileCategorizer();
        //List<String> vsIds=pvs.createVectorStores(rootDir);
        
        
    }

}

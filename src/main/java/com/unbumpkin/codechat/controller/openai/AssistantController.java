package com.unbumpkin.codechat.controller.openai;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.unbumpkin.codechat.dto.openai.Assistant;
import com.unbumpkin.codechat.dto.openai.AssistantTypes;
import com.unbumpkin.codechat.dto.request.ModifyAssistantRequest;
import com.unbumpkin.codechat.repository.openai.AssistantRepository;
import com.unbumpkin.codechat.service.openai.AssistantService;


@RestController
@RequestMapping("/api/v1/assistants/")
public class AssistantController {
    @Autowired
    private AssistantService assistantService;
    @Autowired
    private AssistantRepository assistantRepository;

    
    @PostMapping("{projectId}")
    public ResponseEntity<String> updateAssistant(
        @PathVariable int projectId,
        @RequestBody ModifyAssistantRequest request
    ) throws IOException {
        Assistant assistant= assistantRepository.getAssistantByProjectId(projectId,request.assistantType());

        System.out.println(request.assistantType().toString()+" assistant Request:");
        //printAssistant(request.toOaiModifyAssistantRequest(assistant.instruction()));

        JsonNode assistantOai =assistantService.modifyAsssistant(
            request, assistant.instruction(), assistant.oaiAid()
        );
        if(assistantOai.get("error") != null) {
            return ResponseEntity.badRequest().body(assistantOai.get("error").toString());
        }
        //System.out.println("Assistant OAI:");
        //printJsonNode(assistantOai);
        assistantRepository.updateAssistant(assistant);
        return ResponseEntity.ok().body(assistantOai.toString());
    }
    @GetMapping("{projectId}")
    public ResponseEntity<String> getAssistantByProjectId(
        @PathVariable int projectId,
        @RequestParam(required = false, defaultValue = "codechat") AssistantTypes assistantType
    ) throws JsonProcessingException {
        Assistant assistant = assistantRepository.getAssistantByProjectId(projectId, assistantType);
        if (assistant == null) {
            return ResponseEntity.notFound().build();
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()); // Register JavaTimeModule
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = mapper.writeValueAsString(assistant);
        return ResponseEntity.ok().body(json);
    }
    
    @SuppressWarnings("unused")
    private void printAssistant(
        Map<String, Object> request
    ) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String assistantJson = mapper.writeValueAsString(request);
        System.out.println(assistantJson);
    }
    @SuppressWarnings("unused")
    private void printJsonNode(JsonNode jsonNode) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = mapper.writeValueAsString(jsonNode);
        System.out.println(json);
    }
    

    
}

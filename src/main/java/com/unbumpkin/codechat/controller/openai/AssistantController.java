package com.unbumpkin.codechat.controller.openai;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.unbumpkin.codechat.dto.request.ModifyAssistantRequest;
import com.unbumpkin.codechat.model.openai.Assistant;
import com.unbumpkin.codechat.repository.openai.AssistantRepository;
import com.unbumpkin.codechat.service.openai.AssistantService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


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
        Assistant assistant= assistantRepository.getAssistantByProjectId(projectId);

        System.out.println("Assistant Request:");
        printAssistant(request.toOaiModifyAssistantRequest(assistant.instruction()));

        JsonNode assistantOai =assistantService.modifyAsssistant(
            request, assistant.instruction(), assistant.oaiAid()
        );
        if(assistantOai.get("error") != null) {
            return ResponseEntity.badRequest().body(assistantOai.get("error").toString());
        }
        System.out.println("Assistant OAI:");
        printJsonNode(assistantOai);
        assistantRepository.updateAssistant(assistant);
        return ResponseEntity.ok().body(assistantOai.toString());
    }

    private void printAssistant(
        Map<String, Object> request
    ) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String assistantJson = mapper.writeValueAsString(request);
        System.out.println(assistantJson);
    }
    private void printJsonNode(JsonNode jsonNode) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = mapper.writeValueAsString(jsonNode);
        System.out.println(json);
    }
    

    
}

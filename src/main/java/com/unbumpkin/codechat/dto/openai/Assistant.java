package com.unbumpkin.codechat.dto.openai;

import java.time.LocalDateTime;

import com.unbumpkin.codechat.dto.request.ModifyAssistantRequest;
import com.unbumpkin.codechat.service.openai.AssistantBuilder.ReasoningEfforts;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;

public record Assistant(
    int aid,
    String oaiAid,
    int projectid,
    String name,
    String description,
    String instruction,
    ReasoningEfforts reasoningEffort,
    Models model,
    Float temperature,
    int maxResults,
    int codevsid,
    int markupvsid,
    int configvsid,
    int fullvsid,
    LocalDateTime created
) {
    // Constructor without created timestamp for new assistants
    public Assistant(
        int aid, 
        String oaiAid, 
        int projectid, 
        String name, 
        String description, 
        String instruction,
        ReasoningEfforts reasoningEffort,
        Models model,
        Float temperature,
        int maxResults,
        int codevsid, 
        int markupvsid, 
        int configvsid, 
        int fullvsid
    ) {
        this(aid, oaiAid, projectid, name, description, instruction, reasoningEffort, 
             model, temperature, maxResults, codevsid, markupvsid, configvsid, fullvsid, null);
    }
    public Assistant(Assistant assistant,String newInstruction) {
        this(assistant.aid(), assistant.oaiAid(), assistant.projectid(), assistant.name(), 
             assistant.description(), newInstruction, assistant.reasoningEffort(), 
             assistant.model(), assistant.temperature(), assistant.maxResults(), 
             assistant.codevsid(), assistant.markupvsid(), assistant.configvsid(), 
             assistant.fullvsid(), LocalDateTime.now());
    }
    public Assistant(Assistant assistant, ModifyAssistantRequest request) {
        this(assistant.aid(), assistant.oaiAid(), assistant.projectid(), request.name(), 
             assistant.description(), request.getInstructions(assistant.instruction),
             assistant.reasoningEffort(), assistant.model(), request.temperature(), 
             request.maxResults(), assistant.codevsid(), assistant.markupvsid(), 
             assistant.configvsid(),  assistant.fullvsid(), LocalDateTime.now());
    }
}
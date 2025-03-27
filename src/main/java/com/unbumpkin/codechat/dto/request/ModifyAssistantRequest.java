package com.unbumpkin.codechat.dto.request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.unbumpkin.codechat.model.openai.Assistant;
import com.unbumpkin.codechat.service.openai.AssistantBuilder.ReasoningEfforts;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;

public record ModifyAssistantRequest(
    String name,
    /**
     * The primary function of the assistant. example:
     * You are a code search assistant designed to help users analyze and understand their projects. Your primary role is to provide detailed explanations, code snippets, and actionable suggestions based on the project's files and metadata.
     */
    String primaryFunction,
    /**
     * Adjustable reasoning level. 
     */
    ReasoningEfforts reasoningEffort,
    /**
     * OpenAi model to use
     */
    Models model,
    /**
     * Randomness Level (0 to 2) -
     *   0: Strictly factual,
     *   2: More creative/poetic responses,
     *   Default: 0.7.  
     */
    Float temperature,
    /**
     * The maximum number of results to return. This number should be between 1 and 50 inclusive.
     * Defaults to 10
     */
    int maxResults,
    /**
     * Only for admin (may break the chat)
     * full instruction customization
     */
    String fullInstruction
) {
    public ModifyAssistantRequest {
        if (temperature!=null && (temperature < 0 || temperature > 2.0)) {
            throw new IllegalArgumentException("temperature must be between 0 and 2.0");
        }
        if(maxResults < 1 || maxResults > 50) {
            throw new IllegalArgumentException("Max results must be between 1 and 50");
        }
    }
    private static Pattern functionPattern = Pattern.compile("<Function:[^>]*>");
    private String getInstructions(String existingInstruction) {
        String instruction=existingInstruction;
        if(fullInstruction()!=null && !fullInstruction().isEmpty()) {
            instruction=fullInstruction();
        } else {
            if(primaryFunction()!=null && primaryFunction().isEmpty()) {
                Matcher matcher=functionPattern.matcher(instruction);
                if(matcher.find()) {
                    instruction=matcher.replaceFirst("<Function: "+primaryFunction()+">");
                }
            }
        }

        return instruction;
    }
    public Map<String,Object> toOaiModifyAssistantRequest(String existingInstruction) {
        Map<String,Object> oaiMap = new HashMap<>();
        
        oaiMap.put("description", "Code search assistant for " + name());
        oaiMap.put("instructions", getInstructions(existingInstruction));
        oaiMap.put("model", model.toString());
        oaiMap.put("name", name());
        oaiMap.put("tools", List.of(Map.of(
            "type", "file_search",
            "file_search", Map.of(
                "max_num_results", maxResults()
            )
        )));
        
        if(temperature() != null) {
            oaiMap.put("temperature", temperature());
        }
        if(reasoningEffort != null) {
            oaiMap.put("reasoning_effort", reasoningEffort());
        }
        
        return oaiMap;
    }
    public Assistant toRepoAssistant(Assistant assistant, String existingInstruction) {
        return new Assistant(
            assistant.aid(), assistant.oaiAid(), assistant.projectid(), assistant.name(), assistant.description(),
            getInstructions(existingInstruction), reasoningEffort, model, temperature, maxResults, 
            assistant.codevsid(), assistant.markupvsid(), assistant.configvsid(), assistant.fullvsid()
        );
    }
        
}

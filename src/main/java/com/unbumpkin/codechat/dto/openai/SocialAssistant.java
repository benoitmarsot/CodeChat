package com.unbumpkin.codechat.dto.openai;

import java.time.LocalDateTime;

import com.unbumpkin.codechat.service.openai.AssistantBuilder.ReasoningEfforts;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;

public record SocialAssistant(
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
    int vsid,
    LocalDateTime created
) {
    // Constructor without created timestamp for new assistants
    public SocialAssistant(
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
        int vsid
    ) {
        this(aid, oaiAid, projectid, name, description, instruction, reasoningEffort, 
             model, temperature, maxResults, vsid, null);
    }
}
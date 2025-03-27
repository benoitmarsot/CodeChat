package com.unbumpkin.codechat.model.openai;

import java.time.LocalDateTime;

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
}
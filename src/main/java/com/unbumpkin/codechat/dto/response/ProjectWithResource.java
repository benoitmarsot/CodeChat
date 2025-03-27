package com.unbumpkin.codechat.dto.response;

import java.util.List;

public record ProjectWithResource(
    int projectId, 
    String name, 
    String description, 
    int authorId, 
    int assistantId,
    String assistantModel,
    String assistantDescription,
    List<String> resourceUris
) {
    
}

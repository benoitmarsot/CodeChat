package com.unbumpkin.codechat.dto.response;
import java.util.List;
import java.util.Map;

public record ProjectWithResource(
    int projectId, 
    String name, 
    String description, 
    int authorId, 
    int assistantId,
    String assistantModel,
    String assistantDescription,
    List<Map<String, String>> resources
) {
    
}

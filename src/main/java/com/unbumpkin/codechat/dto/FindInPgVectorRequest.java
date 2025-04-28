package com.unbumpkin.codechat.dto;

import com.unbumpkin.codechat.service.openai.CCProjectFileManager.Types;


public record FindInPgVectorRequest(int projectId, Types type, String content) {
    
}

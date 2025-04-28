package com.unbumpkin.codechat.ai.dto;

import java.util.Map;


public record Chunk(String content, Map<String, String> metadata) {
}

/*

SELECT id, content, metadata
FROM chunks
ORDER BY embedding <-> '[0.1, 0.2, ...]' -- insert your float[] vector here
LIMIT 5;
 */
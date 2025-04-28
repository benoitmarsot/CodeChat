package com.unbumpkin.codechat.ai.dto;

import java.util.Map;

/**
 * Represents an embedded chunk of text with its corresponding metadata.
 *
 * @param content  The original content of the chunk.
 * @param embedding The embedding vector for the chunk.
 * @param metadata  Additional metadata associated with the chunk.
 */
public record EmbeddedChunk(String content, float[] embedding, Map<String, String> metadata) {
}

package com.unbumpkin.codechat.ai.dto;

import java.util.Map;

/**
 * Represents an embedded chunk of text with its corresponding metadata.
 *
 * @param content  The original content of the chunk.
 * @param distance how far is the result from the query vector
 * @param metadata  Additional metadata associated with the chunk.
 */
public record SearchChunkResult(String content, float distance, Map<String, String> metadata) {
}

package com.unbumpkin.codechat.ai.embedder;

import com.unbumpkin.codechat.ai.dto.Chunk;
import com.unbumpkin.codechat.ai.dto.EmbeddedChunk;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public abstract class EmbedderService {
    static public final int MAX_TOKENS = 512;

    public abstract List<EmbeddedChunk> embedChunks(List<Chunk> chunks) throws Exception;

    public abstract float[] embed(String text) throws Exception;

    protected void validateChunk(Chunk chunk) {
        if (chunk.content() == null || chunk.content().isEmpty()) {
            throw new IllegalArgumentException("Chunk content cannot be null or empty");
        }
        if (chunk.metadata() == null) {
            throw new IllegalArgumentException("Chunk metadata cannot be null");
        }
        if (chunk.metadata().get("type") == null) {
            throw new IllegalArgumentException("Chunk metadata must contain a 'type' key");
        }
    }
    

}
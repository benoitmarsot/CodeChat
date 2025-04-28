package com.unbumpkin.codechat.ai.embedder;

import com.unbumpkin.codechat.ai.dto.Chunk;
import com.unbumpkin.codechat.ai.dto.EmbeddedChunk;

import org.springframework.stereotype.Service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.IntArrayList;
import com.knuddels.jtokkit.api.ModelType;

import java.util.List;

@Service
public abstract class EmbedderService {
    static public final int MAX_TOKENS = 8192;

    public abstract List<EmbeddedChunk> embedChunks(List<Chunk> chunks) throws Exception;

    public abstract float[] embed(String text) throws Exception;
    
    public static int getTokenCount(String text) {
        // Tokenize the text using jtokkit
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding enc = registry.getEncodingForModel(ModelType.TEXT_EMBEDDING_ADA_002);
        IntArrayList tokens = enc.encode(text);
        return tokens.size();
    }
    public static boolean isTokenLimitExceeded(String text) {
        int tokenCount = getTokenCount(text);
        return tokenCount > MAX_TOKENS; // Adjust the limit as needed
    }
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
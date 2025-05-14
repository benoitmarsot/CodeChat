package com.unbumpkin.codechat.ai.embedder;

import com.unbumpkin.codechat.ai.dto.Chunk;
import com.unbumpkin.codechat.ai.dto.EmbeddedChunk;

import ai.djl.Application;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PreDestroy;

@Service
@Primary
public class HuggingfaceEmbedderService extends EmbedderService {
    static public final int MAX_TOKENS = 512; 
    private static final HuggingFaceTokenizer BGE_TOKENIZER;
    private final ai.djl.Model model;
    private final Predictor<String, float[]> predictor;

    static {
        try {
            BGE_TOKENIZER = HuggingFaceTokenizer.newInstance("BAAI/bge-base-en-v1.5",Map.of("maxLength","512"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize BGE tokenizer", e);
        }
    }

    public HuggingfaceEmbedderService() throws Exception {
        Criteria<String, float[]> criteria = Criteria.builder()
            .setTypes(String.class, float[].class)
            .optApplication(Application.NLP.TEXT_EMBEDDING)
            .optEngine("PyTorch")
            .optModelUrls("djl://ai.djl.huggingface.pytorch/BAAI/bge-base-en-v1.5")
            .build();
        var model = ModelZoo.loadModel(criteria);
        this.model=model;
        this.predictor = model.newPredictor();
    }


    public static int getTokenCount(String text) {
        return BGE_TOKENIZER.tokenize(text).size();
    }

    public static boolean isTokenLimitExceeded(String text) {
        int tokenCount = getTokenCount(text);
        return tokenCount > MAX_TOKENS; // Adjust the limit as needed
    }
    @Override
    public List<EmbeddedChunk> embedChunks(List<Chunk> chunks) throws Exception {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        List<EmbeddedChunk> embeddedChunks = new ArrayList<>();
        for(Chunk chunk : chunks) {
            this.validateChunk(chunk);
            embeddedChunks.add(new EmbeddedChunk(
                chunk.content(),
                embed(chunk.content()),
                chunk.metadata()
            ));

        }
        return embeddedChunks;
    }

    @Override
    public float[] embed(String text) throws Exception {
        try {
            return predictor.predict(text);
        } catch (Exception e) {
            throw new RuntimeException("Error during embedding: " + e.getMessage(), e);
        }
    }
    @PreDestroy
    public void cleanup() {
        if (predictor != null) {
            predictor.close();
        }
        if (model != null) {
            model.close();
        }
    }

    //@Override
    public float[] embedOld(String text) throws Exception {
        Criteria<String, float[]> criteria = Criteria.builder()
            .setTypes(String.class, float[].class)
            .optApplication(Application.NLP.TEXT_EMBEDDING)
            .optEngine("PyTorch")
            .optModelUrls("djl://ai.djl.huggingface.pytorch/BAAI/bge-base-en-v1.5")
            .build();
    
        try (var model = ModelZoo.loadModel(criteria);
             Predictor<String, float[]> predictor = model.newPredictor()) {
            float[] embedding = predictor.predict(text);
            return embedding;
        } catch (Exception e) {
            throw new RuntimeException("Error during embedding: " + e.getMessage(), e);
        }
    }

}
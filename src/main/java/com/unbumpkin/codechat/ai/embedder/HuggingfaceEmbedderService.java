package com.unbumpkin.codechat.ai.embedder;

import com.unbumpkin.codechat.ai.dto.Chunk;
import com.unbumpkin.codechat.ai.dto.EmbeddedChunk;

import ai.djl.Application;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Primary
public class HuggingfaceEmbedderService extends EmbedderService {
    static public final int MAX_TOKENS = 8192;

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
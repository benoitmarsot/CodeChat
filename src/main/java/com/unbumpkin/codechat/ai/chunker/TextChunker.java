package com.unbumpkin.codechat.ai.chunker;

import org.springframework.stereotype.Component;

import com.unbumpkin.codechat.ai.dto.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TextChunker {

    public List<Chunk> chunk(String text, Map<String,String> metadata) {

        if(text == null || text.isEmpty()) {
            return List.of();
        }
        if (text.length() < 2000) {
            metadata.put("type", "document");
            return List.of(new Chunk(text, metadata));
        }

        List<Chunk> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");
        metadata.put("type", "paragraph");
        for (String p : paragraphs) {
            if (!p.trim().isEmpty()) {
                chunks.add(new Chunk(p, metadata));
            }
        }
        return chunks;
    }
}
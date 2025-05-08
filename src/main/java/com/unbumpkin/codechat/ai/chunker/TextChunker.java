package com.unbumpkin.codechat.ai.chunker;

import org.springframework.stereotype.Component;

import com.unbumpkin.codechat.ai.dto.Chunk;
import com.unbumpkin.codechat.ai.embedder.HuggingfaceEmbedderService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TextChunker extends Chunker {

    private static final int TARGET_CHUNK_SIZE = 500;
    private static final int OVERLAP_SIZE = 150;

    @Override
    public List<Chunk> chunk(String text, String url, Map<String,String> metadata) {

        if(text == null || text.isEmpty()) {
            return List.of();
        }

        // Step 1: Split into segments (paragraphs or sentences)
        List<String> segments = splitIntoSegments(text);
        
        // Step 2 & 3: Merge segments and create overlapping chunks
        return createOverlappingChunks(segments, text, metadata);
    }
    @Override
    public String reconstituteDocument(List<Chunk> chunks) {
        StringBuilder document = new StringBuilder();
        String lastChunkEnd = "";

        for (Chunk chunk : chunks) {
            String chunkText = chunk.content();

            // Remove overlap from the beginning of the current chunk
            if (!lastChunkEnd.isEmpty() && chunkText.startsWith(lastChunkEnd)) {
                chunkText = chunkText.substring(lastChunkEnd.length()).trim();
            }

            if (document.length() > 0) {
                document.append(" ");
            }
            document.append(chunkText);

            // Update the last chunk end for the next iteration
            lastChunkEnd = getOverlapEnd(chunkText);
        }

        return document.toString();
    }

    private List<String> splitIntoSegments(String text) {
        List<String> segments = new ArrayList<>();
        
        // First split by paragraphs
        String[] paragraphs = text.split("\n\n+");
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }
            
            // If paragraph is small enough, keep as is
            if (countHFTokens(paragraph) <= 100) {
                segments.add(paragraph);
            } else {
                // Split large paragraphs into sentences
                String[] sentences = paragraph.split("(?<=[.!?])\\s+");
                for (String sentence : sentences) {
                    if (!sentence.trim().isEmpty()) {
                        segments.add(sentence.trim());
                    }
                }
            }
        }
        
        return segments;
    }

    private List<Chunk> createOverlappingChunks(List<String> segments, String originalText, Map<String, String> metadata) {
        List<Chunk> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int tokenCount = 0;
        int startPosition = 0;
        
        for (String segment : segments) {
            int segmentTokens = countHFTokens(segment);
            
            // If adding this segment would exceed target size and we already have content
            if (tokenCount > 0 && tokenCount + segmentTokens > TARGET_CHUNK_SIZE) {
                // Create a chunk from what we have so far
                String chunkText = currentChunk.toString();
                Map<String, String> chunkMetadata = new HashMap<>(metadata);
                chunkMetadata.put("type", "hybrid-chunk");
                chunkMetadata.put("node_type", "text");
                chunkMetadata.put("chunk_level", "hybrid");
                chunkMetadata.put("start", String.valueOf(startPosition));
                chunkMetadata.put("end", String.valueOf(startPosition + chunkText.length()));
                chunks.add(new Chunk(chunkText, chunkMetadata));
                
                // Start new chunk with overlap
                String overlapText = createOverlap(chunkText);
                currentChunk = new StringBuilder(overlapText);
                tokenCount = countHFTokens(overlapText);
                // Update position for next chunk (with overlap)
                startPosition = startPosition + chunkText.length() - overlapText.length();
                if (startPosition < 0) startPosition = 0;
            }
            
            // Add segment to current chunk
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(segment);
            tokenCount += segmentTokens;
        }
        
        // Add the last chunk if there's anything left
        if (currentChunk.length() > 0) {
            String chunkText = currentChunk.toString();
            Map<String, String> chunkMetadata = new HashMap<>(metadata);
            chunkMetadata.put("type", "hybrid-chunk");
            chunkMetadata.put("node_type", "text");
            chunkMetadata.put("chunk_level", "hybrid");
            chunkMetadata.put("start", String.valueOf(startPosition));
            chunkMetadata.put("end", String.valueOf(startPosition + chunkText.length()));
            chunks.add(new Chunk(chunkText, chunkMetadata));
        }
        
        return chunks;
    }

    private String createOverlap(String text) {
        // Try to find natural boundaries for overlap
        String[] words = text.split("\\s+");
        if (words.length <= OVERLAP_SIZE) {
            return text;
        }
        
        StringBuilder overlap = new StringBuilder();
        for (int i = Math.max(0, words.length - OVERLAP_SIZE); i < words.length; i++) {
            if (overlap.length() > 0) {
                overlap.append(" ");
            }
            overlap.append(words[i]);
        }
        return overlap.toString();
    }
    

    private int countHFTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return HuggingfaceEmbedderService.getTokenCount(text);
        // Simple token estimation using word count as approximation
        // return text.split("\\s+").length;
    }
    private String getOverlapEnd(String text) {
        String[] words = text.split("\\s+");
        if (words.length <= TextChunker.OVERLAP_SIZE) {
            return text;
        }

        StringBuilder overlap = new StringBuilder();
        for (int i = Math.max(0, words.length - TextChunker.OVERLAP_SIZE); i < words.length; i++) {
            if (overlap.length() > 0) {
                overlap.append(" ");
            }
            overlap.append(words[i]);
        }
        return overlap.toString();
    }
}
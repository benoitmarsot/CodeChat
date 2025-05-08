package com.unbumpkin.codechat.ai.chunker;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import com.unbumpkin.codechat.ai.dto.Chunk;

public abstract class Chunker {
    public abstract List<Chunk> chunk(
        String text, String extension, Map<String,String> metadata
    ) throws UnsupportedEncodingException;
    public abstract String reconstituteDocument(List<Chunk> chunks);
}

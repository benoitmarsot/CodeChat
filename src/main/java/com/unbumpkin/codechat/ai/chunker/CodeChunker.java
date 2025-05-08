package com.unbumpkin.codechat.ai.chunker;

import org.springframework.stereotype.Component;

import com.unbumpkin.codechat.ai.dto.Chunk;
import com.unbumpkin.codechat.ai.embedder.HuggingfaceEmbedderService;
import static com.unbumpkin.codechat.ai.embedder.HuggingfaceEmbedderService.MAX_TOKENS;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Correct Tree-sitter Java bindings
import ai.serenade.treesitter.Parser;
import ai.serenade.treesitter.Tree;
import ai.serenade.treesitter.Node;
import ai.serenade.treesitter.Languages;

@Component
public class CodeChunker extends Chunker {

    final static int MAX_NB_CHARS = 2000; // characters, ~4 chars/chunk, adjust as needed

    /**
     * Returns true is the extension and false extension is not supported.
     */
    public static boolean isSupportedExtension(String extension) {
        if(getLanguageByExtension(extension) == -1L){
            return false;
        }
        return true;
    }
    /**
     * Returns the Tree-sitter language code for a given file extension,
     * or -1L if the extension is not supported.
     */
    public static long getLanguageByExtension(String extension) {
        return switch (extension) {
            case "agda" -> Languages.agda();
            case "sh", "bash" -> Languages.bash();
            case "c" -> Languages.c();
            case "cs", "csharp" -> Languages.cSharp();
            case "cpp", "cc", "cxx", "c++" -> Languages.cpp();
            case "css" -> Languages.css();
            case "dart" -> Languages.dart();
            case "elm" -> Languages.elm();
            case "embeddedtemplate" -> Languages.embeddedTemplate();
            case "eno" -> Languages.eno();
            case "go" -> Languages.go();
            case "hs", "haskell" -> Languages.haskell();
            case "html", "htm" -> Languages.html();
            case "java" -> Languages.java();
            case "js", "javascript" -> Languages.javascript();
            case "jl", "julia" -> Languages.julia();
            case "kt", "kts", "kotlin" -> Languages.kotlin();
            case "lua" -> Languages.lua();
            case "md", "markdown" -> Languages.markdown();
            case "ml", "ocaml" -> Languages.ocaml();
            case "php" -> Languages.php();
            case "py", "python" -> Languages.python();
            case "rb", "ruby" -> Languages.ruby();
            case "rs", "rust" -> Languages.rust();
            case "scala" -> Languages.scala();
            case "scss" -> Languages.scss();
            case "swift" -> Languages.swift();
            case "toml" -> Languages.toml();
            case "tsx" -> Languages.tsx();
            case "ts", "typescript" -> Languages.typescript();
            case "vue" -> Languages.vue();
            case "yaml", "yml" -> Languages.yaml();
            case "wasm" -> Languages.wasm();
            default -> -1L;
        };
    }
    @Override
    public List<Chunk> chunk(String code, String extension, Map<String, String> metadata) throws UnsupportedEncodingException {
        long lang = getLanguageByExtension(extension);
        if (lang == -1L) {
            throw new UnsupportedEncodingException("Unsupported file extension: " + extension);
        }
        List<Chunk> chunks = new ArrayList<>();
        Parser parser = new Parser();
        parser.setLanguage(lang);
        
        Tree tree = parser.parseString(code);
        Node root = tree.getRootNode();
    
        // Start recursive chunking
        chunkNodeRecursively(root, code, metadata, chunks);
    
        parser.close();
        tree.close();
        return chunks;
    }
    @Override
    public String reconstituteDocument(List<Chunk> chunks) {
        // Sort chunks by their original start offset (from metadata)
        chunks.sort((a, b) -> {
            int startA = Integer.parseInt(a.metadata().getOrDefault("start", "0"));
            int startB = Integer.parseInt(b.metadata().getOrDefault("start", "0"));
            return Integer.compare(startA, startB);
        });

        StringBuilder document = new StringBuilder();
        for (Chunk chunk : chunks) {
            document.append(chunk.content());
        }
        return document.toString();
    }

    /**
     * Recursively chunk nodes at class, method, or block level, or by size.
     */
    private void chunkNodeRecursively(Node node, String code, Map<String, String> parentMetadata, List<Chunk> chunks) {
        String type = node.getType();
        int start = node.getStartByte();
        int end = node.getEndByte();
        String stChunk = code.substring(start, end);
    
        // If node is small enough, chunk as is
        if (!isTokenLimitExceeded(stChunk) ) {
            Map<String, String> chunkMetadata = new java.util.HashMap<>(parentMetadata);
            chunkMetadata.put("node_type", type);
            chunkMetadata.put("start", String.valueOf(start));
            chunkMetadata.put("end", String.valueOf(end));
            if ("class_declaration".equals(type) || "interface_declaration".equals(type) || "enum_declaration".equals(type)) {
                chunkMetadata.put("chunk_level", "class");
            } else if ("method_declaration".equals(type) || "constructor_declaration".equals(type) || "function_declaration".equals(type)) {
                chunkMetadata.put("chunk_level", "method");
            } else if ("block".equals(type)) {
                chunkMetadata.put("chunk_level", "block");
            } else {
                chunkMetadata.put("chunk_level", "other");
            }
            chunks.add(new Chunk(stChunk, chunkMetadata));
            return;
        }
    
        // Try to chunk children at logical boundaries
        boolean chunked = false;
        for (int i = 0; i < node.getChildCount(); i++) {
            Node child = node.getChild(i);
            String childType = child.getType();
            if (
                "class_declaration".equals(childType) ||
                "interface_declaration".equals(childType) ||
                "enum_declaration".equals(childType) ||
                "method_declaration".equals(childType) ||
                "constructor_declaration".equals(childType) ||
                "function_declaration".equals(childType) ||
                "block".equals(childType)
            ) {
                chunkNodeRecursively(child, code, parentMetadata, chunks);
                chunked = true;
            }
        }
    
        // If no logical children were chunked, or node is still too large, split by size
        if (!chunked) {
            // Fallback: split by size
            int pos = start;
            while (pos < end) {
                int chunkEnd = Math.min(pos + MAX_NB_CHARS, end);
                String chunk=code.substring(pos, chunkEnd); 
                int nbTokens=HuggingfaceEmbedderService.getTokenCount(chunk);
                if(nbTokens>MAX_TOKENS) {
                    chunkEnd -= (nbTokens-MAX_TOKENS)*4; // Adjust chunkEnd based on token count
                }

                Map<String, String> chunkMetadata = new java.util.HashMap<>(parentMetadata);
                chunkMetadata.put("node_type", type);
                chunkMetadata.put("chunk_level", "size");
                chunkMetadata.put("start", String.valueOf(pos));
                chunkMetadata.put("end", String.valueOf(chunkEnd));
                chunks.add(new Chunk(code.substring(pos, chunkEnd), chunkMetadata));
                pos = chunkEnd;
            }
        }
    }
    private boolean isTokenLimitExceeded(String text) {
        // Check if the token limit is exceeded
        return HuggingfaceEmbedderService.isTokenLimitExceeded(text);
    }
    @SuppressWarnings("unused")
    private String getNodeName(Node node, String code) {
        // Get the name of the node (e.g., method name)
        String name = "";
        for (int i = 0; i < node.getChildCount(); i++) {
            Node possibleName = node.getChild(i);
            if ("identifier".equals(possibleName.getType())) {
                name = code.substring(possibleName.getStartByte(), possibleName.getEndByte());
                break;
            }
        }
        return name;
    }

    

}
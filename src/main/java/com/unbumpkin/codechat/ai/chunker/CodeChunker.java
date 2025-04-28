package com.unbumpkin.codechat.ai.chunker;

import org.springframework.stereotype.Component;

import com.unbumpkin.codechat.ai.dto.Chunk;

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
public class CodeChunker {

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
    public List<Chunk> chunk(String code,String extension, Map<String,String> metadata) throws UnsupportedEncodingException {
        long lang = getLanguageByExtension(extension);
        if (lang == -1L) {
            throw new UnsupportedEncodingException("Unsupported file extension: " + extension);
        }
        List<Chunk> chunks = new ArrayList<>();
        Parser parser = new Parser();
        parser.setLanguage(lang);

        Tree tree = parser.parseString(code);
        Node root = tree.getRootNode();

        // Traverse the AST and extract method declarations
        
        //extractMethods(root, code, chunks);
        // Package/imports
        // Each class/interface/enum
        //   Each method/function
        extractMethods(root, code, metadata, chunks); 
        //   (Optionally) fields
        parser.close();
        tree.close();
        return chunks;
    }
    /**
     * Extracts all method/function nodes from the AST and adds them as chunks.
     * Each chunk contains the method's source code and metadata.
     */
    private void extractMethods(Node root, String code, Map<String,String> metadata, List<Chunk> chunks) {
        int childCount = root.getChildCount();
        metadata.put("type", "method");
        for (int i = 0; i < childCount; i++) {
            Node child = root.getChild(i);
            String type = child.getType();
            // TODO: need to find the method name
            String name= ""; // child.getFieldName();

            // For Java, method_declaration and constructor_declaration are common
            if ("method_declaration".equals(type) || "constructor_declaration".equals(type) || "function_declaration".equals(type)) {
                int start = child.getStartByte();
                int end = child.getEndByte();
                String methodSource = code.substring(start, end);
                //child.getChildByFieldName("name").getText(code);
                metadata.put("name", name);
                chunks.add(new Chunk(methodSource, metadata));
            }

            // Recursively search in child nodes
            //extractMethods(child, code, metadata, chunks);
        }
    }

    

}
package com.unbumpkin.codechat.service.openai;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProgramVectorStores {
    public enum Types {
        code, markup, config, full
    }
    public static final Set<String> CodeExtensions = Set.of(
        "py", "js", "jsx", "java", "cs", "php", "rb", "swift", "go", "c", "cpp", "rs", "ts", 
        "kt", "r", "scala", "pl", "lua", "groovy", "ps1", "sh", "clj", "ex", "erl", 
        "hs", "ml", "fs", "sql", "dart", "asm", "bat", "cob", "coffee", "d", "f", 
        "f90", "for", "h", "hpp", "jl", "lisp", "m", "pas", "p", "p6", "pro", "rkt", 
        "vb", "vbs", "vhdl", "wsdl", "xsl", "xslt", "zsh", "cmd", "bash", "csh", "tcsh"
    );

    public static final Set<String> MarkupExtensions = Set.of(
        "html", "htm", "xhtml", "xml", "svg", "css", "scss", "less", "sass", "json", "md", "txt",
        "rtf", "csv", "tsv", "log"
    );

    public static final Set<String> ConfigExtensions = Set.of(
        "yml", "yaml", "json", "xml", "ini", "cfg", "conf", "properties", "toml", "env", "envrc", 
        "config", "plist", "hcl", "tf", "tfvars", "psm1", "psc1", "reg", "inf", "info"
    );
    public static final Set<String> AllExtensions = Stream.of(
            CodeExtensions, MarkupExtensions, ConfigExtensions
        ).flatMap(Set::stream)
        .collect(Collectors.toSet());

    private final Map<Types, Set<File>> fileSetMap;

    public ProgramVectorStores() {
        this.fileSetMap = new HashMap<>();
        fileSetMap.put(Types.code, new HashSet<>());
        fileSetMap.put(Types.config, new HashSet<>());
        fileSetMap.put(Types.markup, new HashSet<>());
    }
    // public List<String> createVectorStores(String dirName) throws IOException {
    //     addDir(dirName);

    //     VectorStore vectorStore = new VectorStore();
    //     List<String> vectorStoreIds = new ArrayList<>(3);
    //     OaiFileService oaiFileService = new OaiFileService();

    //     for(Types type : Types.values()) {
    //         if (fileSetMap.get(type).isEmpty()) {
    //             continue;
    //         }
    //         // Store files using oaiFileService
    //         for (File file : fileSetMap.get(type)) {
    //             OaiFile oaiFile = new OaiFile(file.getName(), file.getAbsolutePath(), type.name());
    //             oaiFileService.storeOaiFile(oaiFile);
    //         }
            

    //         String vsid = vectorStore.createVectorStore(type.name());
    //         VectorStoreFileBatch vectorStoreFileBatch = new VectorStoreFileBatch(vsid);
    //         vectorStoreFileBatch.createBatch(
    //             fileSetMap.get(type).stream().map(
    //                 File::getAbsolutePath
    //         ).collect(Collectors.toList()));
    //         vectorStoreIds.add(vsid);
    //     }
        
    //     return vectorStoreIds;
    // }
    public void addDir(String dirName) throws IOException {
        File dir = new File(dirName);
        if (dir.exists() && dir.isDirectory()) {
            List<File> files = FileUtils.listFiles(dirName, AllExtensions);
            if (files != null) {
                for (File file : files) {
                    addFile(file);
                }
            }
        }
    }
    public void addFile(File file) {
        String extension = FileUtils.getFileExtension(file);
        if (CodeExtensions.contains(extension)) {
            fileSetMap.get(Types.code).add(file);
        } else if (MarkupExtensions.contains(extension)) {
            fileSetMap.get(Types.markup).add(file);
        } else if (ConfigExtensions.contains(extension)) {
            fileSetMap.get(Types.config).add(file);
        }
    }

}
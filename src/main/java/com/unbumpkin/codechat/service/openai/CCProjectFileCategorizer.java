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

import com.unbumpkin.codechat.util.FileUtils;

public class CCProjectFileCategorizer {
    public enum Types {
        code, markup, config, all
    }

    public static final Set<String> CodeExtensions = Set.of(
        "py", "js", "java", "c", "cpp", "php", "rb", "go", "ts",
        "jsx", "cs", "swift", "rs", 
        "kt", "r", "scala", "pl", "lua", "groovy", "ps1", "sh", "clj", "ex", "erl", 
        "hs", "ml", "fs", "sql", "dart", "asm", "bat", "cob", "coffee", "d", "f", 
        "f90", "for", "hpp", "jl", "lisp", "m", "pas", "p", "p6", "pro", "rkt", 
        "vb", "vbs", "vhdl", "wsdl", "xsl", "xslt", "zsh", "cmd", "bash", "csh", "tcsh"
    );
    public static final Set<String> MarkupExtensions = Set.of(
        "html", "htm", "css", "md", "txt", "csv", "rst", "adoc",
        "doc", "docx", "pdf", "pptx", "tex"
    );

    public static final Set<String> ConfigExtensions = Set.of(
        "json" , "xml",
        "yml", "yaml", "ini", "cfg", "conf", "properties", "toml", "env", "envrc", 
        "config", "plist", "hcl", "tf", "tfvars", "psm1", "psc1", "reg", "inf", "info"
    );
    public static final Set<String> AllExtensions = Stream.of(
            CodeExtensions, MarkupExtensions, ConfigExtensions
        ).flatMap(Set::stream)
        .collect(Collectors.toSet()
    );

    private final Map<Types, Set<File>> fileSetMap;

    public CCProjectFileCategorizer() {
        this.fileSetMap = new HashMap<>();
        fileSetMap.put(Types.code, new HashSet<>());
        fileSetMap.put(Types.config, new HashSet<>());
        fileSetMap.put(Types.markup, new HashSet<>());
    }
    public String addDir(String dirName) throws IOException {
        File dir = new File(dirName);
        if (dir.exists() && dir.isDirectory()) {
            List<File> files = FileUtils.listFiles(dirName, AllExtensions);
            if (files != null) {
                for (File file : files) {
                    addFile(file);
                }
            }
        }
        return dir.getAbsolutePath();
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
    public Set<File> getFileSetMap(Types type) {
        return this.fileSetMap.get(type);
    }
    public Set<File> getAllFiles() {
        return Stream.of(Types.values())
            .flatMap(type -> fileSetMap.get(type).stream())
            .collect(Collectors.toSet());
    }

}
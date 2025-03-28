package com.unbumpkin.codechat.util;

import java.io.File;
import java.util.Map;
import java.util.Set;

import com.unbumpkin.codechat.dto.FileRenameDescriptor;

public class ExtMimeType {
    /*
     * OpenAI' SearchAssistant API does support them only
     * See https://platform.openai.com/docs/assistants/tools/file-search/supported-files
     * */

    public static final Set<String> OAI_EXT = Set.of(
        "c", "cpp", "cs", "css", "doc", "docx", "go", "html", "java", "js", 
        "json", "md", "pdf", "php", "pptx", "py", "rb", "tex", "ts", "txt"
    );
        
    public static final Map<String, String> MIME_TYPES = Map.ofEntries(
        // Code files
        Map.entry("py", "text/x-python"),
        Map.entry("js", "application/javascript"),
        Map.entry("java", "text/x-java-source"),
        Map.entry("c", "text/x-c"),
        Map.entry("cpp", "text/x-c++src"),
        Map.entry("php", "application/x-php"),
        Map.entry("rb", "application/x-ruby"),
        Map.entry("go", "text/x-go"),
        Map.entry("ts", "application/typescript"),
        Map.entry("jsx", "text/jsx"),
        Map.entry("cs", "text/x-csharp"),
        Map.entry("swift", "text/x-swift"),
        Map.entry("rs", "text/rust"),
        Map.entry("kt", "text/x-kotlin"),
        Map.entry("r", "text/x-r"),
        Map.entry("scala", "text/x-scala"),
        Map.entry("pl", "text/x-perl"),
        Map.entry("lua", "text/x-lua"),
        Map.entry("groovy", "text/x-groovy"),
        Map.entry("ps1", "text/x-powershell"),
        Map.entry("sh", "text/x-sh"),
        Map.entry("clj", "text/x-clojure"),
        Map.entry("ex", "text/x-elixir"),
        Map.entry("erl", "text/x-erlang"),
        Map.entry("hs", "text/x-haskell"),
        Map.entry("ml", "text/x-ocaml"),
        Map.entry("fs", "text/x-fsharp"),
        Map.entry("sql", "application/sql"),
        Map.entry("dart", "text/x-dart"),
        Map.entry("asm", "text/x-asm"),
        Map.entry("bat", "text/x-bat"),
        Map.entry("cob", "text/x-cobol"),
        Map.entry("coffee", "text/x-coffeescript"),
        Map.entry("d", "text/x-d"),
        Map.entry("f", "text/x-fortran"),
        Map.entry("f90", "text/x-fortran"),
        Map.entry("for", "text/x-fortran"),
        Map.entry("hpp", "text/x-c++hdr"),
        Map.entry("jl", "text/x-julia"),
        Map.entry("lisp", "text/x-common-lisp"),
        Map.entry("m", "text/x-objc"),
        Map.entry("pas", "text/x-pascal"),
        Map.entry("p", "text/x-pascal"),
        Map.entry("p6", "text/x-perl6"),
        Map.entry("pro", "text/x-prolog"),
        Map.entry("rkt", "text/x-racket"),
        Map.entry("vb", "text/x-vb"),
        Map.entry("vbs", "text/vbscript"),
        Map.entry("vhdl", "text/x-vhdl"),
        Map.entry("wsdl", "application/wsdl+xml"),
        Map.entry("xsl", "application/xslt+xml"),
        Map.entry("xslt", "application/xslt+xml"),
        Map.entry("zsh", "text/x-zsh"),
        Map.entry("cmd", "text/x-cmd"),
        Map.entry("bash", "text/x-shellscript"),
        Map.entry("csh", "text/x-csh"),
        Map.entry("tcsh", "text/x-tcsh"),
        
        // Markup files
        Map.entry("html", "text/html"),
        Map.entry("htm", "text/html"),
        Map.entry("css", "text/css"),
        Map.entry("json", "application/json"),
        Map.entry("md", "text/markdown"),
        Map.entry("txt", "text/plain"),
        Map.entry("csv", "text/csv"),
        Map.entry("rst", "text/x-rst"),
        Map.entry("adoc", "text/asciidoc"),
        
        // Config files
        Map.entry("xml", "application/xml"),
        Map.entry("yml", "text/yaml"),
        Map.entry("yaml", "text/yaml"),
        Map.entry("ini", "text/plain"),
        Map.entry("cfg", "text/plain"),
        Map.entry("conf", "text/plain"),
        Map.entry("properties", "text/plain"),
        Map.entry("toml", "application/toml"),
        Map.entry("env", "text/plain"),
        Map.entry("envrc", "text/plain"),
        Map.entry("config", "text/plain"),
        Map.entry("plist", "application/x-plist"),
        Map.entry("hcl", "text/plain"),
        Map.entry("tf", "text/plain"),
        Map.entry("tfvars", "text/plain"),
        Map.entry("psm1", "text/x-powershell"),
        Map.entry("psc1", "text/x-powershell"),
        Map.entry("reg", "text/plain"),
        Map.entry("inf", "text/plain"),
        Map.entry("info", "text/plain"),
        
        // Document files
        Map.entry("doc", "application/msword"),
        Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        Map.entry("pdf", "application/pdf"),
        Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
        Map.entry("tex", "application/x-tex")
    );
    public static String getMimeType(String ext) {
        return MIME_TYPES.get(ext);
    }
    public static boolean oaiSupported(String ext) {
        return OAI_EXT.contains(ext);
    }
    public static FileRenameDescriptor oaiRename(File file) {
        String ext = FileUtils.getFileExtension(file);
        String oldFileName=file.getName();
        String oldFilePath=file.getPath();
        File renamedFile = file;
        if (!oaiSupported(ext)) {
            // Rename the file with a .txt extension
            renamedFile = new File(file.getParent(), file.getName() + ".txt");
            if (file.renameTo(renamedFile)) {
                file = renamedFile; // Update the file reference
            }            
        } 
        return new FileRenameDescriptor(oldFileName, oldFilePath, renamedFile, getMimeType(ext));
    }
    
}

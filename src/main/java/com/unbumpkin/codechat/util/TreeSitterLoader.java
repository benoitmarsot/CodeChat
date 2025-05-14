package com.unbumpkin.codechat.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class TreeSitterLoader {
    
    public static void loadTreeSitterLibraries() {
        try {
            String os = getOsName();
            String arch = getArchName();
            String libraryPath = "/lib/" + os + "-" + arch + "/";
            
            // Load core Tree-sitter library first
            loadLibraryFromResources(libraryPath, "libtree-sitter");
            
            // Load language parsers
            String[] languages = {
                // Missing:
                // "dart", "elm", "kotlin", "lua", "markdown", "ocaml", "php", "typescript", "yaml"
                "agda", "bash", "c", "c-sharp", "cpp", "css", 
                "go", "haskell", "html", "java", "javascript", 
                "julia","python", "ruby", "rust", "scala", "swift", "toml"
                
            };
            
            for (String lang : languages) {
                try {
                    System.out.println("Loading parser for " + lang);
                    loadLibraryFromResources(libraryPath, "libtree-sitter-" + lang);
                } catch (Exception e) {
                    System.err.println("Failed to load parser for " + lang + ": " + e.getMessage());
                    // Continue with other languages
                }
            }
            
            System.out.println("Tree-sitter libraries loaded successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to initialize Tree-sitter: " + e.getMessage());
        }
    }
    /**
     * Returns the library extension for the current OS.
     * @return The library extension for the current OS.
     */
    public static String getOsExtension() {
        String extension;
        if (getOsName().equals("windows")) {
            extension = ".dll";
        } else if (getOsName().equals("darwin")) {
            extension = ".dylib";
        } else {
            extension = ".so";
        }
        
        return extension;
    }
    /**
     * Loads a native library from the resources folder.
     * @param libraryName The name of the library without extension.
     * @throws IOException If an error occurs while loading the library.
     */

    public static void loadLibraryFromResources(String libraryName) throws IOException {
        String os = getOsName();
        String arch = getArchName();
        String libraryPath = "/lib/" + os + "-" + arch + "/";
        
        // Load the library
        loadLibraryFromResources(libraryPath, libraryName);
    }
    /**
     * Loads a native library from the resources folder.
     * @param folder The folder in the resources where the library is located.
     * @param libraryName The name of the library without extension.
     * @throws IOException If an error occurs while loading the library.
     */
    private static void loadLibraryFromResources(String folder, String libraryName) throws IOException {
        String extension=getOsExtension();
        
        String resourcePath = folder + libraryName + extension;
        
        // Extract to temp file
        Path tempFile = Files.createTempFile(libraryName, extension);
        File file = tempFile.toFile();
        file.deleteOnExit(); // This will delete the file when the JVM exits

        try (InputStream in = TreeSitterLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Library not found: " + resourcePath);
            }
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Make sure the file is executable
        file.setExecutable(true);
        
        // Load the library
        System.load(file.getAbsolutePath());
    }

    private static String getOsName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return "darwin";
        } else if (os.contains("linux")) {
            return "linux";
        } else if (os.contains("windows")) {
            return "windows";
        }
        throw new UnsupportedOperationException("Unsupported OS: " + os);
    }
    
    private static String getArchName() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.equals("aarch64") || arch.contains("arm64")) {
            return "arm64";
        } else if (arch.contains("amd64") || arch.contains("x86_64")) {
            return "x64";
        }
        throw new UnsupportedOperationException("Unsupported architecture: " + arch);
    }
    
}
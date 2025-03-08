package com.unbumpkin.codechat.util;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.file.*;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class ZipFileUtils implements AutoCloseable {
    private final ZipFile zipFile;
    private final Map<String, ZipEntry> pathToEntryMap;

    public ZipFileUtils(String zipPath) throws IOException {
        this.zipFile = new ZipFile(zipPath);
        this.pathToEntryMap = new HashMap<>();
        // Index all entries by their path
        zipFile.entries().asIterator().forEachRemaining(entry -> 
            pathToEntryMap.put(entry.getName(), entry)
        );
    }

    public List<String> listFiles(Set<String> extensions) {
        List<String> filePaths = new ArrayList<>();
        for (ZipEntry entry : pathToEntryMap.values()) {
            if (!entry.isDirectory()) {
                String extension = getFileExtension(entry.getName());
                if (extensions.contains(extension)) {
                    filePaths.add(entry.getName());
                }
            }
        }
        return filePaths;
    }

    public byte[] getFileContent(String filePath) throws IOException {
        ZipEntry entry = pathToEntryMap.get(filePath);
        if (entry == null) {
            throw new FileNotFoundException("File not found in ZIP: " + filePath);
        }

        try (InputStream is = zipFile.getInputStream(entry)) {
            return is.readAllBytes();
        }
    }

    public int countLines(String filePath) throws IOException {
        byte[] content = getFileContent(filePath);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(content)))) {
            int lines = 0;
            while (reader.readLine() != null) lines++;
            return lines;
        }
    }

    public RequestBody createRequestBody(String filePath, MediaType mediaType) throws IOException {
        return RequestBody.create(getFileContent(filePath), mediaType);
    }

    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return filename.substring(lastIndexOf + 1);
    }

    public void close() throws IOException {
        zipFile.close();
    }

    // Example usage in OaiFileService:
    public static void example() throws IOException {
        try (ZipFileUtils zipUtils = new ZipFileUtils("path/to/your.zip")) {
            // List all Java files
            List<String> javaFiles = zipUtils.listFiles(Set.of("java"));
            
            // Process a specific file
            String filePath = "path/in/zip/File.java";
            int lineCount = zipUtils.countLines(filePath);
            RequestBody fileBody = zipUtils.createRequestBody(filePath, MediaType.parse("application/json"));
        }
    }
}
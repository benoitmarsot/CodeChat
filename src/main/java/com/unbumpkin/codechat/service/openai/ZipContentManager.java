package com.unbumpkin.codechat.service.openai;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;

import com.unbumpkin.codechat.util.FileUtils;

public class ZipContentManager extends CCProjectFileManager {
    
    private Path tempDir;
    
    public ZipContentManager() throws IOException {
        super();
        this.tempDir = Files.createTempDirectory("zip-extract-");
    }
    
    public String extractZip(byte[] zipContent) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(zipContent))) {
            ZipEntry entry = zipIn.getNextEntry();
            
            while (entry != null) {
                if (!entry.isDirectory()) {
                    String fileName = entry.getName();
                    String extension = FileUtils.getFileExtension(fileName);
                    
                    // Only process files with extensions we care about
                    if (AllExtensions.contains(extension)) {
                        File outputFile = new File(tempDir.toFile(), fileName);
                        
                        // Create parent directories if needed
                        outputFile.getParentFile().mkdirs();
                        
                        try (FileOutputStream out = new FileOutputStream(outputFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = zipIn.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }
                        
                        // Add the file to our manager
                        addFile(outputFile);
                    }
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
        
        return tempDir.toString();
    }
    
    public String getTempDir() {
        return tempDir.toString();
    }
    
    public void cleanUp() {
        try {
            FileUtils.deleteDirectory(tempDir.toFile());
        } catch (IOException e) {
            System.err.println("Failed to delete temporary directory: " + e.getMessage());
        }
    }
}
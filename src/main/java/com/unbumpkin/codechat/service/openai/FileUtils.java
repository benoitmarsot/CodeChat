package com.unbumpkin.codechat.service.openai;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static List<String> listFiles(String rootDir, String extension) {
        List<String> filePaths = new ArrayList<>();
        File root = new File(rootDir);
        if (root.exists() && root.isDirectory()) {
            findFiles(root, extension, filePaths);
        }
        return filePaths;
    }
    public static long getFileSize(String filePath) throws IOException {
        return Files.size(Paths.get(filePath));
    }

    private static void findFiles(File dir, String extension, List<String> filePaths) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findFiles(file, extension, filePaths);
                } else if (file.isFile() && file.getName().endsWith(extension)) {
                    filePaths.add(file.getAbsolutePath());
                }
            }
        }
    }
    
}

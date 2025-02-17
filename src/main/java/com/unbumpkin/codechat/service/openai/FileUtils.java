package com.unbumpkin.codechat.service.openai;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FileUtils {

    public static List<File> listFiles(
        String rootDir, Set<String> extensions
    ) throws IOException {
        List<File> filePaths = new ArrayList<>();
        File root = new File(rootDir);
        if (root.exists() && root.isDirectory()) {
            findFiles(root, extensions, filePaths);
        }
        return filePaths;
    }

    private static void findFiles(
        File dir, Set<String> extensions, List<File> filePaths
    ) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findFiles(file, extensions, filePaths);
                } else if (file.isFile() ) {
                    if(
                        extensions.contains(getFileExtension(file))
                        && file.length() > 0
                    ) {
                        filePaths.add(file);
                    }
                }
            }
        }
    }
    public static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return name.substring(lastIndexOf+1);
    }
    public static int countLines(String path) throws IOException {
        return countLines(new File(path));
    }
    public static int countLines(File file) throws IOException {
        int lines = 0;
        try (java.util.Scanner scanner = new java.util.Scanner(file)) {
            while (scanner.hasNextLine()) {
                scanner.nextLine();
                lines++;
            }
        }
        return lines;
    }    
}

package com.unbumpkin.codechat.dto;

import java.util.List;

public record GitHubChangeTracker(
    String outputDirectory,
    List<GitHubChange> changes,
    List<String> addedFiles,
    List<String> deletedFiles,
    int totalFiles
) {
    public enum ChangeTypes {
        ADDED,
        MODIFIED,
        DELETED
    }
    public static record GitHubChange(
        String path,
        ChangeTypes changeType
    ) {}
}

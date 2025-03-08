package com.unbumpkin.codechat.dto.request;

public record UploadDirRequest(
    String rootDir, 
    String extension, 
    String purpose
) {}

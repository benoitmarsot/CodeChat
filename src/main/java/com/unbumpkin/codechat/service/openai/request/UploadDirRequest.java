package com.unbumpkin.codechat.service.openai.request;

public record UploadDirRequest(
    String rootDir, 
    String extension, 
    String purpose
) {}

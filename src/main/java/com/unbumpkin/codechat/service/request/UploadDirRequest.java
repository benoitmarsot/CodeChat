package com.unbumpkin.codechat.service.request;

public record UploadDirRequest(
    String rootDir, 
    String extension, 
    String purpose
) {}

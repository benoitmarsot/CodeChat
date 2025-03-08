package com.unbumpkin.codechat.dto.request;

public record UploadFileRequest(
    String filepath, 
    String purpose
) {}
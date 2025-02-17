package com.unbumpkin.codechat.service.openai.request;

public record UploadFileRequest(
    String filepath, 
    String purpose
) {}
package com.unbumpkin.codechat.service.request;

public record UploadFileRequest(
    String filepath, 
    String purpose
) {}
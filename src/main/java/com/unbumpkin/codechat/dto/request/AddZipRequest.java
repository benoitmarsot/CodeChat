package com.unbumpkin.codechat.dto.request;


public record AddZipRequest(
    int projectId, String zipName, byte[] zipContent
) {
    
}

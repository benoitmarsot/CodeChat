package com.unbumpkin.codechat.dto.request;

public record AddOaiThreadRequest(String oaiThreadId, Integer vsid, int did, String type) {
}

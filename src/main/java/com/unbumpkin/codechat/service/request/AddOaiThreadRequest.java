package com.unbumpkin.codechat.service.request;

public record AddOaiThreadRequest(String oaiThreadId, Integer vsid, int did, String type) {
}

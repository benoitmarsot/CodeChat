package com.unbumpkin.codechat.dto.request;

import java.util.Map;

public record CreateVSFileRequest( String file_id, Map<String,String> attributes) {}


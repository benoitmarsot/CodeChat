package com.unbumpkin.codechat.dto.request;

import java.util.List;

public record AddWebRequest(
    int projectId,
    String seedUrl,
    int maxPages,
    int maxDepth,
    int requestsPerMinute,
    boolean includeImages, 
    boolean includeDocuments,
    List<String> allowedDomains,
    String userName,
    String password
) {}
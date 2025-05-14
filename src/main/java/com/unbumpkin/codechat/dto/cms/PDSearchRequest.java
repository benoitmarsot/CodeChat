package com.unbumpkin.codechat.dto.cms;

public record PDSearchRequest(
    String query,
    int start,
    int limit
) {
    // This class is a data transfer object (DTO) for the search request.
    // It contains the search query, pagination start index, and limit for the number of results.
}

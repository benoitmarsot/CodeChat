package com.unbumpkin.codechat.dto.cms;

import java.util.List;
/**
 * AiRefArticle is a data transfer object (DTO) that represents an AI reference article.
 * It contains fields for the article's title, teaser body, full body, author name, primary keywords,
 * original URLs, publication date, list of authors, and images.
 */
public record AiRefArticle(
    String name,
    String description,
    List<String> authors,
    List<String> primary,
    List<String> originalUrls,
    String publicationDate,
    List<String> images
) {
    
}

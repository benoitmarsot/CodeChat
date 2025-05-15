package com.unbumpkin.codechat.dto.cms;

import java.util.List;
/**
 * ArticleReferences is a data transfer object (DTO) that represents a collection of articles
 * along with an answer and an overall description.
 * It contains fields for the answer, overall description, and a list of articles.
 */
public record ArticleReferences(
    String answer,
    String overallDescription,
    List<AiRefArticle> articles
) {
}

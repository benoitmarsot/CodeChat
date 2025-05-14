package com.unbumpkin.codechat.dto.cms;

import java.util.List;

public record ArticleReferences(
    String overallDescription,
    List<AiRefArticle> articles
) {
}

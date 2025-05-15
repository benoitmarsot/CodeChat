package com.unbumpkin.codechat.dto.cms;

import java.util.List;

public record SearchedArticle( 
    String title, String teaserBody, String Body, String name, List<String> primary, List<String> originalUrls,
    String publicationdate, List<String> authors, List<String> images ) {
    
}

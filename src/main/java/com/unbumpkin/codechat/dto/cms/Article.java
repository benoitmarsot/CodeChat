package com.unbumpkin.codechat.dto.cms;

import java.util.List;

public record Article( 
    String title, String teaserBody, String Body, String name, List<String> primary, List<String> OriginalUrls,
    String publicationdate, List<String> authors, List<String> images ) {
    
}

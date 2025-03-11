package com.unbumpkin.codechat.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


import com.unbumpkin.codechat.model.openai.OaiFile;

public class AnswerUtils {
    private static final Pattern FILE_ID_PATTERN = java.util.regex.Pattern.compile("file-([\\w]+)\\s");
    public static Set<String> getReferencesFileIds(String answerStr) {
        Set<String> fileIds = new HashSet<>();
        Matcher matcher = FILE_ID_PATTERN.matcher(answerStr);
        // Get the "answers" array
        while (matcher.find()) {
            // This will print the captured file ID (the part after "file-").
            fileIds.add(matcher.group(1));
        }
        
        return fileIds;
    }
    public static String replaceferencesFileIds(String jsonString, List<OaiFile> refFiles) {
        for(OaiFile refFile : refFiles) {
            jsonString = jsonString.replace(refFile.fileId(), "file://"+refFile.filePath());
        };
        return jsonString;
    }
    
}

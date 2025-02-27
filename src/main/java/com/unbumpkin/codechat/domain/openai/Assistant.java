package com.unbumpkin.codechat.domain.openai;

public record Assistant(
    int aid,
    String oaiAid,
    String name,
    String description,
    int projectid,
    int codevsid,
    int markupvsid,
    int configvsid,
    int fullvsid
) {
}
package com.unbumpkin.codechat.model;

import com.unbumpkin.codechat.model.UserSecret.Labels;
import java.util.Map;

public record ProjectResource(
    int prId,
    int projectId, 
    String uri,
    ResTypes resType,
    Map<Labels,UserSecret> secrets
) {
    public enum ResTypes {git, web, file, zip, slack, discord};
}

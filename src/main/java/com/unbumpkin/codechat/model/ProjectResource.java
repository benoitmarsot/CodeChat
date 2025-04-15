package com.unbumpkin.codechat.model;

import com.unbumpkin.codechat.model.UserSecret.Labels;
import java.util.Map;

public record ProjectResource(
    int prId,
    int projectId, 
    String uri,
    ResTypes restType,
    Map<Labels,UserSecret> secrets
) {
    public enum ResTypes {git, file, zip, slack};
}

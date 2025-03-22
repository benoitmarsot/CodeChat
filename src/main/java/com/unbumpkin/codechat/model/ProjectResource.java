package com.unbumpkin.codechat.model;

import com.unbumpkin.codechat.model.UserSecret.Labels;
import java.util.Map;

public record ProjectResource(
    int prId,
    int projectId, 
    String uri,
    Map<Labels,UserSecret> secrets
) {}

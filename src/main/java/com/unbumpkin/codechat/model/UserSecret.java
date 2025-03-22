package com.unbumpkin.codechat.model;

public record UserSecret(int userid, Labels label, String value) {
    public enum Labels { username, password, pat, branch, commitHash }
    public UserSecret(Labels label, String value) {
        this(-1, label, value);
    }
}


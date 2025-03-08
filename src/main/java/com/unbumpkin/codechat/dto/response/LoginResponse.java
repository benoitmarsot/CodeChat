package com.unbumpkin.codechat.dto.response;

import com.unbumpkin.codechat.model.User;

public record LoginResponse(String token, User user) {}
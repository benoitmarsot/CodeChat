package com.unbumpkin.codechat.controller;

import com.unbumpkin.codechat.domain.User;
import com.unbumpkin.codechat.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        return authService.register(user);
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> request) {
        String token = authService.login(request.get("email"), request.get("password"));
        return Map.of("token", token);
    }
}

/*
 * register: 
 * curl -X POST http://localhost:8080/auth/register -H "Content-Type: application/json" -d '{"name":"Benoit Marsot","email":"benoit@benoitmarsot.com","password":"password","role":"ADMIN"}'
 * login:
 * curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"email":"benoit@benoitmarsot.com","password":"password"}'
 * Access a protected endpoint:
 * curl -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJiZW5vaXRAYmVub2l0bWFyc290LmNvbSIsImV4cCI6MTczOTM2MjgxNn0.8RglC3YaMn_q3P76o1ANDN4wpVLn6lcDax9246tRNLk" http://localhost:8080/api/openai/files
 */
package com.unbumpkin.codechat.controller;

import com.unbumpkin.codechat.domain.LoginResponse;
import com.unbumpkin.codechat.exception.DuplicateEmailException;
import com.unbumpkin.codechat.service.AuthService;
import com.unbumpkin.codechat.service.auth.AuthRequest;
import com.unbumpkin.codechat.service.auth.RegisterRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import javax.naming.AuthenticationException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest user) {
        logger.debug("Received registration request for email: {}", user.email());
        System.out.println("Received registration request for user: " + user.email());  // Add debug logging
        authService.register(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<String> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody AuthRequest request) throws AuthenticationException {
        LoginResponse response = authService.login(request.email(), request.password());
        return ResponseEntity.ok(Map.of(
            "token", response.token(),
            "userId", String.valueOf(response.user().userid())
        ));
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", ex.getMessage()));
    }
}
/*
 * register: 
 * curl -X POST http://localhost:8080/api/v1/register -H "Content-Type: application/json" -d '{"name":"Benoit Marsot","email":"benoit@benoitmarsot.com","password":"password","role":"ADMIN"}'
 * login:
 * curl -X POST http://localhost:8080/api/v1/login -H "Content-Type: application/json" -d '{"email":"benoit@benoitmarsot.com","password":"password"}'
 * Access a protected endpoint:
 * curl -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJiZW5vaXRAYmVub2l0bWFyc290LmNvbSIsImV4cCI6MTczOTM2MjgxNn0.8RglC3YaMn_q3P76o1ANDN4wpVLn6lcDax9246tRNLk" http://localhost:8080/api/openai/files
 */
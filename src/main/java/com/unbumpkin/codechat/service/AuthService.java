package com.unbumpkin.codechat.service;

import com.unbumpkin.codechat.dto.auth.RegisterRequest;
import com.unbumpkin.codechat.dto.response.LoginResponse;
import com.unbumpkin.codechat.model.User;
import com.unbumpkin.codechat.repository.UserRepository;
import com.unbumpkin.codechat.security.JwtUtil;
import com.unbumpkin.codechat.util.ValidationUtil;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public ResponseEntity<String> register(RegisterRequest userRequest) {
        ValidationUtil.validate(userRequest);
        User user=new User(0,userRequest.name(),userRequest.email(),passwordEncoder.encode(userRequest.password()),userRequest.role());
        userRepository.create(user);
        return ResponseEntity.ok("User registered successfully");
    }
    public LoginResponse login(String email, String password) throws AuthenticationException {
        User user = userRepository.findByEmail(email);
        if(user==null||!passwordEncoder.matches(password, user.password())) {
            throw new AuthenticationException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user);
        
        // Create authentication token
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        return new LoginResponse(token, user);
    }

    public String refreshToken(String refreshToken) throws AuthenticationException {
        String subject=jwtUtil.validateToken(refreshToken);
        if (subject==null || subject.isEmpty() ) {
            throw new AuthenticationException("Invalid refresh token");
        }
        
        User user = userRepository.findByEmail(subject);
        
        if (user == null) {
            throw new AuthenticationException("User not found");
        }
        
        return jwtUtil.generateToken(user);
    }

}
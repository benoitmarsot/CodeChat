package com.unbumpkin.codechat.service;

import com.unbumpkin.codechat.domain.User;
import com.unbumpkin.codechat.repository.UserRepository;
import com.unbumpkin.codechat.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

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

    public String register(User user) {
        //user.setPassword(passwordEncoder.encode(user.password()));
        user=new User(user.id(),user.name(),user.email(),passwordEncoder.encode(user.password()),user.role());
        userRepository.create(user);
        return "User registered successfully";
    }

    public String login(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().password())) {
            return jwtUtil.generateToken(email);
        }
        throw new RuntimeException("Invalid credentials");
    }
}
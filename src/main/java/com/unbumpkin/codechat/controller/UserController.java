package com.unbumpkin.codechat.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.unbumpkin.codechat.model.User;
import com.unbumpkin.codechat.repository.UserRepository;
import com.unbumpkin.codechat.security.JwtUtil;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final JwtUtil jwtUtil;

    public UserController(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }
    
    @GetMapping
    public ResponseEntity<Iterable<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }    

    @GetMapping("/current-user")
    public ResponseEntity<Map<String,String>> getCurrentUserId(
        @RequestHeader("Authorization") String authHeader
    ) {
        String token=authHeader.substring(7);
        int id =jwtUtil.getUserIdFromToken(token);
        User user = userRepository.findById(id);
        return ResponseEntity.ok(
            Map.of("userid", String.valueOf(user.userid()),
            "name", user.name(),
            "email", user.email(),
            "created", user.created().toString(),
            "accountNonExpired", user.isAccountNonExpired()?"true":"false",
            "role", user.role().toString()
        ));


    }
}



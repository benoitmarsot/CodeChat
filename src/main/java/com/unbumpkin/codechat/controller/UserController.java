package com.unbumpkin.codechat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.unbumpkin.codechat.domain.Greeting;
import com.unbumpkin.codechat.domain.User;
import com.unbumpkin.codechat.repository.UserRepository;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;


@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @Autowired
    private final UserRepository userRepository;


    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @GetMapping
    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }    

    @PostMapping
    public Optional<User> createUser(@RequestBody User user) {
        return userRepository.create(user);
    }

    @GetMapping("/count")
    public int count() {
        return userRepository.count();
    }
    

    @GetMapping("/greeting")
    public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return new Greeting(counter.incrementAndGet(), String.format(template, name));
    }
}
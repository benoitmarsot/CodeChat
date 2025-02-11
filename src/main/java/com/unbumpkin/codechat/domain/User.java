package com.unbumpkin.codechat.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record User(
    int id, 
    @NotBlank(message = "Name is mandatory")
    String name, 
    @NotBlank(message = "Name is mandatory")
    @Email(message = "Email should be valid")
    String email,
    @NotBlank(message = "Password is mandatory")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", 
        message = "Password should contain at least one digit, one lowercase, one uppercase, one special character and should be at least 8 characters long")
    String password,
    Role role
) {
    public enum Role {
        USER, ADMIN
    }
    //here we can add custom methods to the record
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
    
    
}
package com.unbumpkin.codechat.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.unbumpkin.codechat.domain.User;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${JWT_SECRET_KEY}")
    private String secretKey;

    @Value("${JWT_EXPIRATION_TIME}")
    private long expirationTime;

    @PostConstruct
    public void init() {
        System.out.println("Secret Key: " + secretKey);
        System.out.println("Expiration Time: " + expirationTime);
    }
    public String generateToken(String email) {
        return JWT.create()
                .withSubject(email)
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))
                .sign(Algorithm.HMAC256(secretKey));
    }

    public String validateToken(String token) {
        DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secretKey))
                .build()
                .verify(token);
        return jwt.getSubject();
    }
    public String generateToken(User user) {
        return JWT.create()
                .withSubject(user.email())
                .withClaim("userId", user.userid())
                .withClaim("role", user.role().name())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))
                .sign(Algorithm.HMAC256(secretKey));
    }

    public DecodedJWT decodeToken(String token) {
        return JWT.require(Algorithm.HMAC256(secretKey))
                .build()
                .verify(token);
    }

    public String validateTokenAndGetEmail(String token) {
        return decodeToken(token).getSubject();
    }
    public int getUserIdFromToken(String token) {
        return decodeToken(token).getClaim("userId").asInt();
    }

    public String getRoleFromToken(String token) {
        return decodeToken(token).getClaim("role").asString();
    }
}
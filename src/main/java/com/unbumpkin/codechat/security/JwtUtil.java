package com.unbumpkin.codechat.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${JWT_SECRET_KEY}")
    private String secretKey;

    @Value("${JWT_EXPIRATION_TIME}")
    private long expirationTime;

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
}
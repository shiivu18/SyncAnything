package com.unlimsync.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

public class JwtUtil {
    // In a real deployment the secret should be loaded from env vars / secret manager
    private static final Key KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_MS = 15 * 60 * 1000; // 15 minutes

    public static String generateToken(String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + EXPIRATION_MS))
                .signWith(KEY)
                .compact();
    }

    public static String validateAndGetSubject(String token) {
        return Jwts.parser()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}

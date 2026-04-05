package com.appdefend.backend.security;

import com.appdefend.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes());
    }

    public String generateAccessToken(AppUserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getAccessTokenMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuer(properties.getIssuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .id(UUID.randomUUID().toString())
            .claims(Map.of(
                "uid", userDetails.getUser().id(),
                "roles", userDetails.getUser().roles(),
                "permissions", userDetails.getUser().permissions(),
                "views", userDetails.getUser().views()))
            .signWith(key)
            .compact();
    }

    public String generateRefreshToken(AppUserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getRefreshTokenDays(), ChronoUnit.DAYS);
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuer(properties.getIssuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .id(UUID.randomUUID().toString())
            .claim("type", "refresh")
            .claim("uid", userDetails.getUser().id())
            .signWith(key)
            .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, AppUserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject().equals(userDetails.getUsername()) && claims.getExpiration().after(new Date());
    }

    public long getAccessTokenExpirySeconds() {
        return properties.getAccessTokenMinutes() * 60;
    }
}

package com.appdefend.backend.security;

import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenStoreService {
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String REVOKED_PREFIX = "revoked:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public TokenStoreService(StringRedisTemplate redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    public void storeRefreshToken(String refreshToken) {
        Claims claims = jwtService.extractAllClaims(refreshToken);
        long ttlSeconds = Duration.between(Instant.now(), claims.getExpiration().toInstant()).getSeconds();
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(REFRESH_PREFIX + claims.getId(), claims.getSubject(), Duration.ofSeconds(ttlSeconds));
        }
    }

    public boolean isRefreshTokenValid(String refreshToken) {
        Claims claims = jwtService.extractAllClaims(refreshToken);
        String value = redisTemplate.opsForValue().get(REFRESH_PREFIX + claims.getId());
        return value != null && value.equals(claims.getSubject());
    }

    public void revokeRefreshToken(String refreshToken) {
        Claims claims = jwtService.extractAllClaims(refreshToken);
        redisTemplate.delete(REFRESH_PREFIX + claims.getId());
    }

    public void revokeAccessToken(String accessToken) {
        Claims claims = jwtService.extractAllClaims(accessToken);
        long ttlSeconds = Duration.between(Instant.now(), claims.getExpiration().toInstant()).getSeconds();
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(REVOKED_PREFIX + claims.getId(), "1", Duration.ofSeconds(ttlSeconds));
        }
    }

    public boolean isAccessTokenRevoked(String accessToken) {
        Claims claims = jwtService.extractAllClaims(accessToken);
        return Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_PREFIX + claims.getId()));
    }
}

package com.forum.forum.security;

import com.forum.forum.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class TokenService {

    // Dep. injection from application.properties
    @Value("${forum.jwt.expiration}")
    private String expiration;

    @Value("${forum.jwt.secret}")
    private String secret;

    public String generateToken(Authentication authentication) {
        User userLogged = (User) authentication.getPrincipal();
        Date today = new Date();
        Date expeditionDate = new Date(today.getTime() + Long.parseLong(expiration));

        return Jwts.builder()
                .issuer("Forum API")
                .subject(userLogged.getId().toString())
                .issuedAt(today)
                .expiration(expeditionDate)
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    public Boolean isTokenValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(parse(token).getSubject());
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

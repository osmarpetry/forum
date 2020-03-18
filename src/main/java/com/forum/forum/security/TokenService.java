package com.forum.forum.security;

import com.forum.forum.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TokenService {

    // Dep. injection from application.properties
    @Value("${forum.jwt.expiration}")
    private String expiration;

    @Value("${forum.jwt.secret")
    private String secret;

    public String generateToken(Authentication authentication) {
        User userLogged = (User) authentication.getPrincipal();
        Date today = new Date();
        Date expeditionDate = new Date(today.getTime() + Long.parseLong(expiration));

        return Jwts.builder()
                .setIssuer("Forum API")
                .setSubject(userLogged.getId().toString())
                .setIssuedAt(today)
                .setExpiration(expeditionDate)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();

    }
}

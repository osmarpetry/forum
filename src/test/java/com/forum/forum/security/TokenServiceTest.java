package com.forum.forum.security;

import com.forum.forum.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "expiration", "86400000");
        ReflectionTestUtils.setField(tokenService, "secret", "a-test-secret-with-at-least-32-bytes!!");
    }

    @Test
    void generatedTokenIsValidAndCarriesUserId() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 42L);

        String token = tokenService.generateToken(new UsernamePasswordAuthenticationToken(user, null));

        assertTrue(tokenService.isTokenValid(token));
        assertEquals(42L, tokenService.getUserId(token));
    }

    @Test
    void garbageAndNullTokensAreInvalid() {
        assertFalse(tokenService.isTokenValid("not.a.jwt"));
        assertFalse(tokenService.isTokenValid(null));
    }
}

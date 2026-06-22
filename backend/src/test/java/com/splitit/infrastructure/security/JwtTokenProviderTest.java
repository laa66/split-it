package com.splitit.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.splitit.domain.user.model.User;
import io.jsonwebtoken.JwtException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-must-be-at-least-32-bytes-long-aaaa";
    private static final String OTHER_SECRET = "another-secret-also-at-least-32-bytes-long-bbb";

    private User sampleUser() {
        return new User(UUID.randomUUID(), "alice@example.com", "Alice", "HASH", OffsetDateTime.now());
    }

    @Test
    void generatesValidTokenAndExtractsClaims() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 24);
        User user = sampleUser();

        String token = provider.generateToken(user);

        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(user.getId());
        assertThat(provider.getEmail(token)).isEqualTo(user.getEmail());
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, -1);
        String token = provider.generateToken(sampleUser());

        assertThat(provider.isValid(token)).isFalse();
        assertThatThrownBy(() -> provider.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtTokenProvider issuer = new JwtTokenProvider(OTHER_SECRET, 24);
        JwtTokenProvider verifier = new JwtTokenProvider(SECRET, 24);

        String token = issuer.generateToken(sampleUser());

        assertThat(verifier.isValid(token)).isFalse();
        assertThatThrownBy(() -> verifier.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 24);
        String token = provider.generateToken(sampleUser());
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThat(provider.isValid(tampered)).isFalse();
    }

    @Test
    void rejectsGarbageToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 24);

        assertThat(provider.isValid("not.a.jwt")).isFalse();
    }
}

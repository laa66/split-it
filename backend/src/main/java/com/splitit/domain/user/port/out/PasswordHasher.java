package com.splitit.domain.user.port.out;

/**
 * Domain port for password hashing. Keeps the domain free of Spring Security types.
 * The infrastructure adapter (BCrypt) implements this.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}

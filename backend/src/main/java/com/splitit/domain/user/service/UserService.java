package com.splitit.domain.user.service;

import com.splitit.domain.user.exception.EmailAlreadyUsedException;
import com.splitit.domain.user.exception.InvalidCredentialsException;
import com.splitit.domain.user.exception.InvalidRegistrationException;
import com.splitit.domain.user.model.User;
import com.splitit.domain.user.port.in.AuthenticateUserUseCase;
import com.splitit.domain.user.port.in.RegisterUserUseCase;
import com.splitit.domain.user.port.out.PasswordHasher;
import com.splitit.domain.user.port.out.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Pure domain service. Depends only on out-ports (UserRepository, PasswordHasher),
 * never on Spring or persistence types. Wired as a bean in infrastructure config.
 */
public class UserService implements RegisterUserUseCase, AuthenticateUserUseCase {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final int MAX_DISPLAY_NAME_LENGTH = 100;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /**
     * Real bcrypt (cost 12) hash used only to perform a dummy password comparison when the
     * email is unknown, so authentication takes constant time regardless of user existence
     * (mitigates timing-based account enumeration). It never matches any real credential.
     */
    private static final String DUMMY_HASH =
            "$2a$12$WaztSN4RArANZTWx6969Oedd9GTN4LRHIewnaBkitk7iYA2hcWqOe";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User register(RegisterCommand command) {
        String email = normalizeEmail(command.email());
        String displayName = command.displayName() == null ? null : command.displayName().trim();
        String rawPassword = command.rawPassword();

        validate(email, displayName, rawPassword);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }

        String passwordHash = passwordHasher.hash(rawPassword);
        return userRepository.save(User.newUser(email, displayName, passwordHash));
    }

    @Override
    public User authenticate(AuthenticateCommand command) {
        String email = normalizeEmail(command.email());
        String rawPassword = command.rawPassword();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Dummy match against a constant hash to equalize timing with the existing-user path.
            passwordHasher.matches(rawPassword == null ? "" : rawPassword, DUMMY_HASH);
            throw new InvalidCredentialsException();
        }

        if (rawPassword == null || !passwordHasher.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    private void validate(String email, String displayName, String rawPassword) {
        if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidRegistrationException("Invalid email format");
        }
        if (displayName == null || displayName.isEmpty()) {
            throw new InvalidRegistrationException("Display name must not be empty");
        }
        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new InvalidRegistrationException(
                    "Display name must be at most " + MAX_DISPLAY_NAME_LENGTH + " characters");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidRegistrationException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new InvalidRegistrationException(
                    "Password must be at most " + MAX_PASSWORD_BYTES + " bytes");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}

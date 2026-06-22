package com.splitit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splitit.domain.user.exception.EmailAlreadyUsedException;
import com.splitit.domain.user.exception.InvalidCredentialsException;
import com.splitit.domain.user.exception.InvalidRegistrationException;
import com.splitit.domain.user.model.User;
import com.splitit.domain.user.port.in.AuthenticateUserUseCase.AuthenticateCommand;
import com.splitit.domain.user.port.in.RegisterUserUseCase.RegisterCommand;
import com.splitit.domain.user.port.out.PasswordHasher;
import com.splitit.domain.user.port.out.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        passwordHasher = org.mockito.Mockito.mock(PasswordHasher.class);
        userService = new UserService(userRepository, passwordHasher);
    }

    private User persisted(String email, String displayName, String hash) {
        return new User(UUID.randomUUID(), email, displayName, hash, OffsetDateTime.now());
    }

    @Test
    void register_hashesPasswordAndSaves() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> persisted("alice@example.com", "Alice", "HASHED"));

        User result = userService.register(new RegisterCommand("alice@example.com", "Alice", "password123"));

        verify(passwordHasher).hash("password123");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("HASHED");
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void register_normalizesEmailToLowercaseAndTrims() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordHasher.hash(anyString())).thenReturn("HASHED");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> persisted("alice@example.com", "Alice", "HASHED"));

        userService.register(new RegisterCommand("  Alice@Example.com ", "Alice", "password123"));

        verify(userRepository).existsByEmail("alice@example.com");
    }

    @Test
    void register_emailTaken_throwsConflictAndDoesNotSave() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.register(new RegisterCommand("taken@example.com", "Bob", "password123")))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).save(any());
        verify(passwordHasher, never()).hash(anyString());
    }

    @Test
    void register_shortPassword_throwsValidationAndDoesNotTouchRepository() {
        assertThatThrownBy(() ->
                userService.register(new RegisterCommand("alice@example.com", "Alice", "short")))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessageContaining("at least 8");

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordOver72Bytes_throwsValidationAndDoesNotSave() {
        // 73 single-byte chars -> 73 bytes, over the bcrypt 72-byte limit.
        String tooLong = "a".repeat(73);

        assertThatThrownBy(() ->
                userService.register(new RegisterCommand("alice@example.com", "Alice", tooLong)))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessageContaining("72 bytes");

        verify(userRepository, never()).save(any());
        verify(passwordHasher, never()).hash(anyString());
    }

    @Test
    void register_passwordOver72BytesViaMultibyteChars_throwsValidation() {
        // 25 four-byte emoji = 100 bytes but only 25 code points; char count alone would pass.
        String multibyte = "😀".repeat(25);

        assertThatThrownBy(() ->
                userService.register(new RegisterCommand("alice@example.com", "Alice", multibyte)))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessageContaining("72 bytes");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_blankDisplayName_throwsValidation() {
        assertThatThrownBy(() ->
                userService.register(new RegisterCommand("alice@example.com", "  ", "password123")))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessageContaining("Display name");
    }

    @Test
    void register_invalidEmail_throwsValidation() {
        assertThatThrownBy(() ->
                userService.register(new RegisterCommand("not-an-email", "Alice", "password123")))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessageContaining("email");
    }

    @Test
    void register_tooLongDisplayName_throwsValidation() {
        String longName = "x".repeat(101);
        assertThatThrownBy(() ->
                userService.register(new RegisterCommand("alice@example.com", longName, "password123")))
                .isInstanceOf(InvalidRegistrationException.class);
    }

    @Test
    void authenticate_validCredentials_returnsUser() {
        User stored = persisted("alice@example.com", "Alice", "HASHED");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(stored));
        when(passwordHasher.matches("password123", "HASHED")).thenReturn(true);

        User result = userService.authenticate(new AuthenticateCommand("alice@example.com", "password123"));

        assertThat(result).isEqualTo(stored);
    }

    @Test
    void authenticate_unknownEmail_performsDummyMatchThenThrowsInvalidCredentials() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.authenticate(new AuthenticateCommand("ghost@example.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);

        // Constant-time mitigation: a dummy match MUST run to equalize timing with the
        // existing-user path. We only assert it was invoked, never the constant hash value.
        verify(passwordHasher).matches(eq("password123"), anyString());
    }

    @Test
    void authenticate_wrongPassword_throwsInvalidCredentials() {
        User stored = persisted("alice@example.com", "Alice", "HASHED");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(stored));
        when(passwordHasher.matches(eq("wrong"), eq("HASHED"))).thenReturn(false);

        assertThatThrownBy(() ->
                userService.authenticate(new AuthenticateCommand("alice@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}

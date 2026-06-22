package com.splitit.domain.user.port.in;

import com.splitit.domain.user.model.User;

public interface AuthenticateUserUseCase {

    /**
     * Authenticates a user by email and raw password.
     *
     * @throws com.splitit.domain.user.exception.InvalidCredentialsException if email is unknown
     *         or the password does not match (intentionally indistinguishable to the caller)
     */
    User authenticate(AuthenticateCommand command);

    record AuthenticateCommand(String email, String rawPassword) {
    }
}

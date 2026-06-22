package com.splitit.domain.user.port.in;

import com.splitit.domain.user.model.User;

public interface RegisterUserUseCase {

    /**
     * Registers a new user.
     *
     * @throws com.splitit.domain.user.exception.EmailAlreadyUsedException if the email is taken
     * @throws com.splitit.domain.user.exception.InvalidRegistrationException if input is invalid
     */
    User register(RegisterCommand command);

    record RegisterCommand(String email, String displayName, String rawPassword) {
    }
}

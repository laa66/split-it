package com.splitit.domain.user.port.out;

import com.splitit.domain.user.model.User;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Persists a new user and returns it with database-assigned id and createdAt. */
    User save(User user);
}

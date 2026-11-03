package cnu.ssd.pennywise.service;

import cnu.ssd.pennywise.entity.UserEntity;
import cnu.ssd.pennywise.exception.AuthException;
import cnu.ssd.pennywise.model.User;
import cnu.ssd.pennywise.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * Business logic for signup/login. Hashes and verifies
 * passwords with jBCrypt directly, Spring Security is intentionally not used.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates a new account. The raw password never touches the database, only
     * its jBCrypt hash (with a freshly generated salt) is persisted.
     *
     * @throws AuthException if the username or email is already taken
     */
    public User register(String username, String email, String rawPassword) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = email == null ? "" : email.trim();

        if (normalizedUsername.isEmpty() || normalizedEmail.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            throw new AuthException("Username, email and password are all required.");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new AuthException("That username is already taken.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AuthException("That email is already registered.");
        }

        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        UserEntity newUser = new UserEntity();
        newUser.setUsername(normalizedUsername);
        newUser.setEmail(normalizedEmail);
        newUser.setPasswordHash(hash);

        UserEntity saved = userRepository.save(newUser);
        return toModel(saved);
    }

    /**
     * Verifies credentials against the stored jBCrypt hash.
     *
     * @throws AuthException if the username does not exist or the password does not match
     */
    public User login(String username, String rawPassword) {
        UserEntity entity = userRepository.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new AuthException("No account found with that username."));

        if (rawPassword == null || !BCrypt.checkpw(rawPassword, entity.getPasswordHash())) {
            throw new AuthException("Incorrect password.");
        }
        return toModel(entity);
    }

    private User toModel(UserEntity entity) {
        return new User(entity.getId(), entity.getUsername(), entity.getEmail());
    }
}

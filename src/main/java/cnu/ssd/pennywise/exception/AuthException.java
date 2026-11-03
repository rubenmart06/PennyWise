package cnu.ssd.pennywise.exception;

import cnu.ssd.pennywise.service.AuthService;

/**
 * Thrown by {@link AuthService} on a registration or login failure (duplicate
 * username/email, unknown username, wrong password). Carries a message meant
 * to be shown to the user as-is.
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }
}


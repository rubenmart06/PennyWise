package cnu.ssd.pennywise.exception;

import cnu.ssd.pennywise.service.HouseholdService;

/**
 * Thrown by {@link HouseholdService} on a household or membership failure
 * (invalid input, unknown household/user, duplicate membership). Carries a
 * message meant to be shown to the user as-is.
 */
public class HouseholdException extends RuntimeException {

    public HouseholdException(String message) {
        super(message);
    }
}

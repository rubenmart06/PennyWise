package cnu.ssd.pennywise.exception;

import cnu.ssd.pennywise.service.BudgetService;

/**
 * Thrown by {@link BudgetService} on invalid input (the name is blank/duplicate within the household,
 * the limit is negative). Carries a message meant
 * to be shown to the user as-is.
 */

public class BudgetException extends RuntimeException {

    public BudgetException(String message) {
        super(message);
    }
}

package cnu.ssd.pennywise.exception;

import cnu.ssd.pennywise.service.TransactionService;

/**
 * Thrown by {@link TransactionService} on invalid input (bad amount/date,
 * unknown household/payer, an invalid split). Carries a message meant to be
 * shown to the user as-is.
 */
public class TransactionException extends RuntimeException {

    public TransactionException(String message) {
        super(message);
    }
}
package cnu.ssd.pennywise.exception;

import cnu.ssd.pennywise.service.CurrencyService;

/**
 * Thrown by {@link CurrencyService} on invalid input (bad currency code/rate,
 * unknown household). See {@link RateNotConfiguredException} for the specific
 * "no static rate yet" case.
 */
public class CurrencyException extends RuntimeException {

    public CurrencyException(String message) {
        super(message);
    }
}

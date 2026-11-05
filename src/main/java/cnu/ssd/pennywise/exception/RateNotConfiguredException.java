package cnu.ssd.pennywise.exception;

import cnu.ssd.pennywise.service.CurrencyService;

/**
 * Thrown by {@link CurrencyService#convertToPrimary} when the household has
 * no static exchange rate for the requested currency yet.
 * A distinct type so callers, specifically {@code TransactionController},
 * can catch it and prompt for a rate instead of just showing a generic error.
 */
public class RateNotConfiguredException extends CurrencyException {

    private final String currencyCode;

    public RateNotConfiguredException(String currencyCode) {
        super("No exchange rate configured for " + currencyCode + " yet.");
        this.currencyCode = currencyCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }
}

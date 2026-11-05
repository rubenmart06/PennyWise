package cnu.ssd.pennywise.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import cnu.ssd.pennywise.entity.ExchangeRateEntity;
import cnu.ssd.pennywise.entity.HouseholdEntity;
import cnu.ssd.pennywise.exception.CurrencyException;
import cnu.ssd.pennywise.exception.RateNotConfiguredException;
import cnu.ssd.pennywise.model.ExchangeRate;
import cnu.ssd.pennywise.repository.ExchangeRateRepository;
import cnu.ssd.pennywise.repository.HouseholdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for a household's static exchange rates:
 * stores/updates them and converts a foreign-currency amount into that
 * household's primary currency using them.
 */
@Service
public class CurrencyService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final HouseholdRepository householdRepository;

    public CurrencyService(ExchangeRateRepository exchangeRateRepository, HouseholdRepository householdRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.householdRepository = householdRepository;
    }

    @Transactional(readOnly = true)
    public List<ExchangeRate> getRatesForHousehold(Long householdId) {
        return exchangeRateRepository.findByHousehold_Id(householdId).stream()
                .map(e -> new ExchangeRate(e.getCurrencyCode(), e.getRate()))
                .collect(Collectors.toList());
    }

    /**
     * Creates or updates the static rate for one
     * currency. Past transactions already converted with the old rate are
     * left untouched, only future conversions use the new value.
     *
     * @throws CurrencyException if the currency/rate is invalid, the household is unknown,
     *                           or the code is the household's own primary currency
     */
    @Transactional
    public ExchangeRate setRate(Long householdId, String currencyCode, BigDecimal rate) {
        String code = currencyCode == null ? "" : currencyCode.trim().toUpperCase();
        if (code.length() != 3) {
            throw new CurrencyException("Currency must be a 3-letter code.");
        }
        if (rate == null || rate.signum() <= 0) {
            throw new CurrencyException("Rate must be a positive number.");
        }

        HouseholdEntity household = householdRepository.findById(householdId)
                .orElseThrow(() -> new CurrencyException("Unknown household."));
        if (code.equals(household.getPrimaryCurrency())) {
            throw new CurrencyException("Can't set a rate for the household's own primary currency.");
        }

        ExchangeRateEntity entity = exchangeRateRepository.findByHousehold_IdAndCurrencyCode(householdId, code)
                .orElse(null);
        if (entity == null) {
            entity = new ExchangeRateEntity();
            entity.setHousehold(household);
            entity.setCurrencyCode(code);
            entity.setRate(rate);
        } else {
            entity.setRate(rate);
        }
        exchangeRateRepository.save(entity);
        return new ExchangeRate(code, rate);
    }

    /**
     * Converts {@code amount} (in {@code currencyCode}) into the household's
     * primary currency. Returns {@code amount} unchanged when it's already in
     * the primary currency.
     *
     * @throws RateNotConfiguredException if {@code currencyCode} isn't the primary currency
     *                                     and the household has no static rate for it yet
     * @throws CurrencyException          if the household is unknown
     */
    @Transactional(readOnly = true)
    public BigDecimal convertToPrimary(Long householdId, String currencyCode, BigDecimal amount) {
        HouseholdEntity household = householdRepository.findById(householdId)
                .orElseThrow(() -> new CurrencyException("Unknown household."));
        String code = currencyCode == null ? "" : currencyCode.trim().toUpperCase();

        if (code.equals(household.getPrimaryCurrency())) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }

        ExchangeRateEntity rateEntity = exchangeRateRepository.findByHousehold_IdAndCurrencyCode(householdId, code)
                .orElseThrow(() -> new RateNotConfiguredException(code));
        return amount.divide(rateEntity.getRate(), 2, RoundingMode.HALF_UP);
    }
}

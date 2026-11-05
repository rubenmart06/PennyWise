package cnu.ssd.pennywise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

/** A currency code and its static rate against a household's primary currency. */

@Data
@Getter
@AllArgsConstructor
public class ExchangeRate {
    private final String currencyCode;
    private final BigDecimal rate;
}

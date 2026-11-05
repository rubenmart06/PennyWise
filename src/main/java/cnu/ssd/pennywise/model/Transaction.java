package cnu.ssd.pennywise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Client-side view of a financial record: type, date, amount (in both its
 * original currency and the household's primary currency), category,
 * description, payer, scope and (if shared) its splits.
 */

@Data
@Getter
@AllArgsConstructor
public class Transaction {

    private final Long id;
    private final TransactionType type;
    private final LocalDate date;
    private final String category;
    private final String description;
    private final Long payerUserId;
    private final String payerUsername;
    private final TransactionScope scope;
    private final String currencyCode;
    private final BigDecimal originalAmount;
    private final BigDecimal convertedAmount;
    private final List<SplitDetail> splits;

    public boolean isShared() {
        return scope == TransactionScope.SHARED;
    }
}
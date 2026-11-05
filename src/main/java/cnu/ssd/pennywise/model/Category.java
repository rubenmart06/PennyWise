package cnu.ssd.pennywise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

/** A household category: name, type (Income/Expense) and its optional monthly limit. */

@Data
@Getter
@AllArgsConstructor
public class Category {

    private final Long id;
    private final String name;
    private final TransactionType type;
    private final BigDecimal limitAmount;

    public boolean hasLimit() {
        return limitAmount != null;
    }

    @Override
    public String toString() {
        return name;
    }
}

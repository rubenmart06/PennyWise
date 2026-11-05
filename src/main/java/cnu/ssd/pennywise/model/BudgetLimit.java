package cnu.ssd.pennywise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;

import java.math.BigDecimal;

/**
 * One row of the budget overview: a label ("Global limit" or a category
 * name), its monthly limit (null means no limit set) and how much has been
 * spent against it so far this month, in the household's primary currency.
 */

@Data
@Generated
@AllArgsConstructor
public class BudgetLimit {

    private final String label;
    private final BigDecimal limitAmount;
    private final BigDecimal spentAmount;

    public boolean hasLimit() {
        return limitAmount != null && limitAmount.signum() > 0;
    }

    /** Spent / limit, as a plain double for a progress bar; 0 when there's no limit. */
    public double getUsageRatio() {
        return hasLimit() ? spentAmount.doubleValue() / limitAmount.doubleValue() : 0d;
    }
}

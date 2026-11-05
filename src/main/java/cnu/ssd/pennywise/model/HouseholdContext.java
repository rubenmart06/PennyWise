package cnu.ssd.pennywise.model;

import org.springframework.stereotype.Component;

/**
 * Holds the household the user currently has open. Same rationale as
 * {@link Session}: a Spring {@code @Component} singleton rather than a plain
 * POJO, so {@code TransactionController}, {@code CurrencyController} and
 * {@code MainDashboardController} can each simply {@code @Autowired} it
 * instead of passing the current {@link Household} (and its member list)
 * around by hand.
 */
@Component
public class HouseholdContext {

    private Household currentHousehold;

    public Household getCurrentHousehold() {
        return currentHousehold;
    }

    public void setCurrentHousehold(Household household) {
        this.currentHousehold = household;
    }

    public void clear() {
        this.currentHousehold = null;
    }

    public Long getCurrentHouseholdId() {
        return currentHousehold == null ? null : currentHousehold.getId();
    }
}

package cnu.ssd.pennywise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

/** "{@code fromUsername} owes {@code toUsername} {@code amount}", in the household's primary currency. */

@Data
@Getter
@AllArgsConstructor
public class Debt {
    private final Long fromUserId;
    private final String fromUsername;
    private final Long toUserId;
    private final String toUsername;
    private final BigDecimal amount;
}

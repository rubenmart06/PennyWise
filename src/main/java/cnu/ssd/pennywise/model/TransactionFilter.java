package cnu.ssd.pennywise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;

/** Filter/search criteria for the transaction history; every field is optional (null = "any"). */

@Data
@Getter
@AllArgsConstructor
public class TransactionFilter {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final String category;
    private final Long memberUserId;
    private final String keyword;

    public static TransactionFilter none() {
        return new TransactionFilter(null, null, null, null, null);
    }
}

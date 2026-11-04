package cnu.ssd.pennywise.model;

import lombok.*;

import java.util.Collections;
import java.util.List;

/**
 * Client-side view of a household: id, name, kind, primary currency and its
 * current members. Plain data holder.
 */

@Data
@AllArgsConstructor
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class Household {
    private final Long id;
    @ToString.Include
    private final String name;
    private final HouseholdKind kind;
    private final String primaryCurrency;
    private final List<Member> members;

    public int getMemberCount() {
        return members.size();
    }
}

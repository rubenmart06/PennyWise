package cnu.ssd.pennywise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

/** A user's membership within one household: who they are and their role in it. */

@Data
@AllArgsConstructor
@Getter
public class Member {
    private final Long membershipId;
    private final User user;
    private final MemberRole role;

    public boolean isHost() {
        return role == MemberRole.HOST;
    }

    @Override
    public String toString() {
        return user.getUsername();
    }
}

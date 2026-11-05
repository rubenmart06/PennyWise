package cnu.ssd.pennywise.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@AllArgsConstructor
public class Invitation {
    private final Long householdId;
    private final String token;
    private final String link;
    private final InvitationStatus status;
}

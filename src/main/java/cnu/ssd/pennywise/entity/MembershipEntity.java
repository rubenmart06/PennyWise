package cnu.ssd.pennywise.entity;

import cnu.ssd.pennywise.model.MemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity backing the {@code memberships} table, the join between
 * {@link UserEntity} and {@link HouseholdEntity}, carrying the user's
 * {@link MemberRole} within that household. A user can belong to the same
 * household at most once (enforced by the unique constraint below).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "household_id"})
})
public class MembershipEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private HouseholdEntity household;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberRole role;
}

package cnu.ssd.pennywise.repository;

import java.util.List;

import cnu.ssd.pennywise.entity.HouseholdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link HouseholdEntity}.
 */
@Repository
public interface HouseholdRepository extends JpaRepository<HouseholdEntity, Long> {

    /** Every household the given user belongs to, found by joining through {@code MembershipEntity}. */
    @Query("select m.household from MembershipEntity m where m.user.id = :userId")
    List<HouseholdEntity> findAllByMemberUserId(@Param("userId") Long userId);
}

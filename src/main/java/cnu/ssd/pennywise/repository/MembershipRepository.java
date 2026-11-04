package cnu.ssd.pennywise.repository;

import java.util.List;
import java.util.Optional;

import cnu.ssd.pennywise.entity.HouseholdEntity;
import cnu.ssd.pennywise.entity.MembershipEntity;
import cnu.ssd.pennywise.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link MembershipEntity}.
 */
@Repository
public interface MembershipRepository extends JpaRepository<MembershipEntity, Long> {

    List<MembershipEntity> findByHousehold(HouseholdEntity household);

    List<MembershipEntity> findByUser(UserEntity user);

    Optional<MembershipEntity> findByHouseholdAndUser(HouseholdEntity household, UserEntity user);

    boolean existsByHouseholdAndUser(HouseholdEntity household, UserEntity user);
}

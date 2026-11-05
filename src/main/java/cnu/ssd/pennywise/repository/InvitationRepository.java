package cnu.ssd.pennywise.repository;

import cnu.ssd.pennywise.entity.InvitationEntity;
import cnu.ssd.pennywise.model.InvitationStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link InvitationEntity}.
 */

@Repository
public interface InvitationRepository extends JpaRepository<InvitationEntity, Long> {

    Optional<InvitationEntity> findByToken(String token);

    Optional<InvitationEntity> findByHousehold_IdAndStatus(Long householdId, InvitationStatus status);
}

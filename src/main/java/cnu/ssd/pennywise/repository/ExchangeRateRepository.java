package cnu.ssd.pennywise.repository;

import java.util.List;
import java.util.Optional;

import cnu.ssd.pennywise.entity.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link ExchangeRateEntity}.
 */
@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, Long> {

    List<ExchangeRateEntity> findByHousehold_Id(Long householdId);

    Optional<ExchangeRateEntity> findByHousehold_IdAndCurrencyCode(Long householdId, String currencyCode);
}

package cnu.ssd.pennywise.repository;

import java.util.List;

import cnu.ssd.pennywise.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link TransactionEntity}.
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    /** Simple derived lookup by household id. */
    List<TransactionEntity> findByHousehold_Id(Long householdId);

    /**
     * Same lookup, newest first, with {@code payer} eagerly joined so the
     * service can safely read {@code payer.getUsername()} after the query
     * returns without needing an extra open session.
     */
    @Query("select t from TransactionEntity t left join fetch t.payer where t.household.id = :householdId "
            + "order by t.date desc, t.id desc")
    List<TransactionEntity> findByHouseholdIdFetchPayer(@Param("householdId") Long householdId);
}

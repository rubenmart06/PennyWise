package cnu.ssd.pennywise.repository;

import java.time.LocalDate;
import java.util.List;

import cnu.ssd.pennywise.entity.TransactionEntity;
import cnu.ssd.pennywise.model.TransactionScope;
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
    @Query("select t from TransactionEntity t left join fetch t.payer left join fetch t.category "
            + "where t.household.id = :householdId "
            + "order by t.date desc, t.id desc")
    List<TransactionEntity> findByHouseholdIdFetchPayer(@Param("householdId") Long householdId);

    /**
     * Dynamic filter/search (requirement 13): every parameter is optional \u2014
     * pass {@code null} to mean "any". {@code memberUserId} matches a
     * transaction where that member is either the payer or one of its split
     * participants. {@code keyword} matches (case-insensitively) against the
     * category or description.
     */
    @Query("select distinct t from TransactionEntity t left join fetch t.payer left join fetch t.category c left join t.splits s "
            + "where t.household.id = :householdId "
            + "and t.date >= :fromDate and t.date <= :toDate "
            + "and (:category = '' or c.name = :category) "
            + "and (:memberUserId = 0L or t.payer.id = :memberUserId or s.userId = :memberUserId) "
            + "and (:keywordPattern = '' or lower(t.description) like :keywordPattern "
            + "or lower(c.name) like :keywordPattern) "
            + "order by t.date desc, t.id desc")
    List<TransactionEntity> searchWithDefaults(@Param("householdId") Long householdId,
                                               @Param("fromDate") LocalDate fromDate,
                                               @Param("toDate") LocalDate toDate,
                                               @Param("category") String category,
                                               @Param("memberUserId") Long memberUserId,
                                               @Param("keywordPattern") String keywordPattern);

    /**
     * Same filter, keeping the nullable-argument signature the Services use. PostgreSQL cannot
     * infer the type of a null bind parameter ({@code ? is null} fails with "could not determine
     * data type", and {@code lower(?)} sees {@code bytea}), so "no filter" is translated here into
     * non-null neutral values: a very wide date range, {@code ""} and {@code 0}.
     */
    default List<TransactionEntity> search(Long householdId, LocalDate fromDate, LocalDate toDate,
                                           String category, Long memberUserId, String keyword) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.of(1900, 1, 1);
        LocalDate to = toDate != null ? toDate : LocalDate.of(9999, 12, 31);
        String cat = category == null ? "" : category;
        Long member = memberUserId == null ? 0L : memberUserId;
        String pattern = (keyword == null || keyword.isBlank()) ? "" : "%" + keyword.trim().toLowerCase() + "%";
        return searchWithDefaults(householdId, from, to, cat, member, pattern);
    }

    /** Whether any transaction still uses the given category (blocks deleting it). */
    boolean existsByCategory_Id(Long categoryId);

    /** Every shared transaction of a household, payer eagerly joined \u2014 used by {@code DebtSettlementService}. */
    @Query("select distinct t from TransactionEntity t left join fetch t.payer "
            + "where t.household.id = :householdId and t.scope = :scope")
    List<TransactionEntity> findByHouseholdIdAndScopeFetchPayer(@Param("householdId") Long householdId,
                                                                @Param("scope") TransactionScope scope);
}

package cnu.ssd.pennywise.repository;

import cnu.ssd.pennywise.entity.CategoryEntity;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link CategoryEntity}.
 */

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findByHousehold_Id(Long householdId);

    Optional<CategoryEntity> findByHousehold_IdAndName(Long householdId, String name);

    boolean existsByHousehold_IdAndName(Long householdId, String name);
}

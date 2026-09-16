package com.aaryav.finance.repository;

import com.aaryav.finance.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Category entity operations.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Find all categories accessible by user (default + user's custom). */
    @Query("SELECT c FROM Category c WHERE c.user IS NULL OR c.user.id = :userId ORDER BY c.id")
    List<Category> findAllAccessibleByUser(@Param("userId") Long userId);

    /** Find a category by name accessible by user. */
    @Query("SELECT c FROM Category c WHERE c.name = :name AND (c.user IS NULL OR c.user.id = :userId)")
    Optional<Category> findByNameAccessibleByUser(@Param("name") String name, @Param("userId") Long userId);

    /** Find custom category by name and user. */
    @Query("SELECT c FROM Category c WHERE c.name = :name AND c.custom = true AND c.user.id = :userId")
    Optional<Category> findCustomByNameAndUserId(@Param("name") String name, @Param("userId") Long userId);

    /** Check if a custom category with given name exists for user. */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND (c.user IS NULL OR c.user.id = :userId)")
    boolean existsByNameForUser(@Param("name") String name, @Param("userId") Long userId);
}

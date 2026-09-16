package com.aaryav.finance.repository;

import com.aaryav.finance.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Transaction entity operations.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Find transaction by ID and user for ownership check. */
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    /** Find transactions with optional filters, sorted newest first. */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND (:startDate IS NULL OR t.date >= :startDate) " +
           "AND (:endDate IS NULL OR t.date <= :endDate) " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "ORDER BY t.date DESC, t.id DESC")
    List<Transaction> findByFilters(@Param("userId") Long userId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate,
                                    @Param("categoryId") Long categoryId);

    /** Check if any transaction references a category. */
    boolean existsByCategoryId(Long categoryId);

    /** Check if any transaction for a specific user references a category. */
    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.category.id = :categoryId AND t.user.id = :userId")
    boolean existsByCategoryIdAndUserId(@Param("categoryId") Long categoryId, @Param("userId") Long userId);

    /** Get transactions for a user since a start date (for goal progress). */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.date >= :startDate")
    List<Transaction> findByUserIdAndDateOnOrAfter(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);

    /** Get transactions for monthly report. */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND YEAR(t.date) = :year AND MONTH(t.date) = :month")
    List<Transaction> findByUserIdAndYearAndMonth(@Param("userId") Long userId,
                                                   @Param("year") int year,
                                                   @Param("month") int month);

    /** Get transactions for yearly report. */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND YEAR(t.date) = :year")
    List<Transaction> findByUserIdAndYear(@Param("userId") Long userId, @Param("year") int year);
}

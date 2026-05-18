package com.dutchtrip.dutchtrip.domain.expense.repository;

import com.dutchtrip.dutchtrip.domain.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByTripIdOrderByPaymentTimeDesc(Long tripId);

    @Query("SELECT e.title, em.amountOwed FROM Expense e " +
            "JOIN ExpenseMember em ON em.expense.id = e.id " +
            "WHERE e.tripId = :tripId AND em.userId = :userId AND em.amountOwed > 0")
    List<Object[]> findExpenseInfoByTripIdAndUserId(@Param("tripId") Long tripId, @Param("userId") Long userId);
}
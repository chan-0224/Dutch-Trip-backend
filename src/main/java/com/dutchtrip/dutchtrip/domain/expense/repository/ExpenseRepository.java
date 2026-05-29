package com.dutchtrip.dutchtrip.domain.expense.repository;

import com.dutchtrip.dutchtrip.domain.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByTripIdOrderByPaymentTimeDesc(Long tripId);

    List<Expense> findAllByTripIdAndExpenseType(Long tripId, String expenseType);

    @Query("SELECT DISTINCT e.title FROM Expense e " +
            "JOIN ExpenseMember em ON em.expense.id = e.id " +
            "WHERE e.tripId = :tripId AND em.userId = :userId AND em.amountOwed > 0")
    List<String> findTitlesByTripIdAndUserId(@Param("tripId") Long tripId, @Param("userId") Long userId);
}
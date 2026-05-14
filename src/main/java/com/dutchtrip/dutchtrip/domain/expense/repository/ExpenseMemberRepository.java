package com.dutchtrip.dutchtrip.domain.expense.repository;

import com.dutchtrip.dutchtrip.domain.expense.entity.ExpenseMember;
import com.dutchtrip.dutchtrip.domain.settlement.dto.UserBalanceDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseMemberRepository extends JpaRepository<ExpenseMember, Long> {

    @Query("SELECT new com.dutchtrip.dutchtrip.domain.settlement.dto.UserBalanceDto(" +
            "em.userId, (SUM(em.amountPaid) - SUM(em.amountOwed))) " +
            "FROM ExpenseMember em " +
            "JOIN em.expense e " +
            "WHERE e.tripId = :tripId " +
            "GROUP BY em.userId")
    List<UserBalanceDto> findNetBalancesByTripId(@Param("tripId") Long tripId);
}

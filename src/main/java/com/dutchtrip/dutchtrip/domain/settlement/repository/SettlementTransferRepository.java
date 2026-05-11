package com.dutchtrip.dutchtrip.domain.settlement.repository;

import com.dutchtrip.dutchtrip.domain.settlement.entity.SettlementTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementTransferRepository extends JpaRepository<SettlementTransfer, Long> {
    List<SettlementTransfer> findAllByTripId(Long tripId);
    void deleteAllByTripId(Long tripId); // 재정산 시 기존 데이터를 지움
}


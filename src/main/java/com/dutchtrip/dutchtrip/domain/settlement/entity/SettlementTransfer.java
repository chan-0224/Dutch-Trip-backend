package com.dutchtrip.dutchtrip.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlement_transfers")
public class SettlementTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "receiver_user_id", nullable = false)
    private Long receiverUserId;

    @Column(name = "amount_to_send", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountToSend;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public SettlementTransfer(Long tripId, Long senderUserId, Long receiverUserId, BigDecimal amountToSend) {
        this.tripId = tripId;
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
        this.amountToSend = amountToSend;
    }
}

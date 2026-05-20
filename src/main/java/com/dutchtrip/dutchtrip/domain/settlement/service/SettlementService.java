package com.dutchtrip.dutchtrip.domain.settlement.service;

import com.dutchtrip.dutchtrip.domain.expense.entity.ExpenseMember;
import com.dutchtrip.dutchtrip.domain.expense.repository.ExpenseMemberRepository;
import com.dutchtrip.dutchtrip.domain.settlement.dto.TransferResponseDto;
import com.dutchtrip.dutchtrip.domain.settlement.dto.UserBalanceDto;
import com.dutchtrip.dutchtrip.domain.settlement.entity.SettlementTransfer;
import com.dutchtrip.dutchtrip.domain.settlement.repository.SettlementTransferRepository;
import com.dutchtrip.dutchtrip.domain.trip.entity.Trip;
import com.dutchtrip.dutchtrip.domain.trip.repository.TripRepository;
import com.dutchtrip.dutchtrip.domain.user.entity.User;
import com.dutchtrip.dutchtrip.domain.user.repository.UserRepository;
import com.dutchtrip.dutchtrip.global.exception.CustomException;
import com.dutchtrip.dutchtrip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementTransferRepository settlementTransferRepository;
    private final ExpenseMemberRepository expenseMemberRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    private static class PersonBalance {
        Long userId;
        BigDecimal amount;

        PersonBalance(Long userId, BigDecimal amount) {
            this.userId = userId;
            this.amount = amount;
        }
    }

    @Transactional
    @org.springframework.cache.annotation.Cacheable(value = "settlements", key = "#tripId")
    public List<TransferResponseDto> calculateAndGetSettlements(Long tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_FOUND));
        String tripName = trip.getTitle();

        settlementTransferRepository.deleteAllByTripId(tripId);

        List<UserBalanceDto> balances = expenseMemberRepository.findNetBalancesByTripId(tripId);

        PriorityQueue<PersonBalance> creditors = new PriorityQueue<>((a, b) -> b.amount.compareTo(a.amount));
        PriorityQueue<PersonBalance> debtors = new PriorityQueue<>((a, b) -> b.amount.compareTo(a.amount));

        for (UserBalanceDto b : balances) {
            if (b.getNetBalance() == null) continue;

            if (b.getNetBalance().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new PersonBalance(b.getUserId(), b.getNetBalance()));
            } else if (b.getNetBalance().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new PersonBalance(b.getUserId(), b.getNetBalance().abs()));
            }
        }

        List<SettlementTransfer> newTransfers = new ArrayList<>();

        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            PersonBalance debtor = debtors.poll();
            PersonBalance creditor = creditors.poll();

            BigDecimal transferAmount = debtor.amount.min(creditor.amount);

            newTransfers.add(SettlementTransfer.builder()
                    .tripId(tripId)
                    .senderUserId(debtor.userId)
                    .receiverUserId(creditor.userId)
                    .amountToSend(transferAmount)
                    .build());

            debtor.amount = debtor.amount.subtract(transferAmount);
            creditor.amount = creditor.amount.subtract(transferAmount);

            if (debtor.amount.compareTo(BigDecimal.ZERO) > 0) debtors.add(debtor);
            if (creditor.amount.compareTo(BigDecimal.ZERO) > 0) creditors.add(creditor);
        }

        settlementTransferRepository.saveAll(newTransfers);

        Set<Long> involvedUserIds = newTransfers.stream()
                .flatMap(t -> Stream.of(t.getSenderUserId(), t.getReceiverUserId()))
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findAllById(involvedUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Set<Long> senderIds = newTransfers.stream()
                .map(SettlementTransfer::getSenderUserId)
                .collect(Collectors.toSet());

        Map<Long, List<ExpenseMember>> senderDebtsMap = expenseMemberRepository
                .findByUserIdInAndExpenseTripIdAndAmountOwedGreaterThan(senderIds, tripId, BigDecimal.ZERO)
                .stream()
                .collect(Collectors.groupingBy(ExpenseMember::getUserId));

        return newTransfers.stream().map(transfer -> {

            User sender = Optional.ofNullable(userMap.get(transfer.getSenderUserId()))
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            User receiver = Optional.ofNullable(userMap.get(transfer.getReceiverUserId()))
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            List<ExpenseMember> senderDebts = senderDebtsMap.getOrDefault(sender.getId(), Collections.emptyList());

            List<TransferResponseDto.RelatedExpenseDto> relatedExpenseDtos = senderDebts.stream()
                    .map(debt -> TransferResponseDto.RelatedExpenseDto.builder()
                            .expenseTitle(debt.getExpense().getTitle())
                            .amount(debt.getAmountOwed())
                            .build())
                    .collect(Collectors.toList());

            return TransferResponseDto.builder()
                    .sender(new TransferResponseDto.SenderInfo(sender.getId(), sender.getNickname()))
                    .receiver(new TransferResponseDto.ReceiverInfo(
                            receiver.getId(),
                            receiver.getNickname(),
                            receiver.getBankName(),
                            receiver.getAccountNumber()))
                    .amountToSend(transfer.getAmountToSend())
                    .tripName(tripName)
                    .relatedExpenses(relatedExpenseDtos)
                    .build();
        }).collect(Collectors.toList());
    }
}
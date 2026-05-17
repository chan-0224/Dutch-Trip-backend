package com.dutchtrip.dutchtrip.domain.expense.service;

import com.dutchtrip.dutchtrip.domain.expense.dto.ExpenseDto;
import com.dutchtrip.dutchtrip.domain.expense.entity.Expense;
import com.dutchtrip.dutchtrip.domain.expense.entity.ExpenseItem;
import com.dutchtrip.dutchtrip.domain.expense.entity.ExpenseItemParticipant;
import com.dutchtrip.dutchtrip.domain.expense.entity.ExpenseMember;
import com.dutchtrip.dutchtrip.domain.expense.repository.ExpenseMemberRepository;
import com.dutchtrip.dutchtrip.domain.expense.repository.ExpenseRepository;
import com.dutchtrip.dutchtrip.domain.trip.entity.Trip;
import com.dutchtrip.dutchtrip.domain.trip.repository.TripMemberRepository;
import com.dutchtrip.dutchtrip.domain.trip.repository.TripRepository;
import com.dutchtrip.dutchtrip.domain.user.entity.User;
import com.dutchtrip.dutchtrip.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import com.dutchtrip.dutchtrip.global.exception.CustomException;
import com.dutchtrip.dutchtrip.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseMemberRepository expenseMemberRepository;
    private final UserRepository userRepository;

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;

    // OCR 분석 (임시 - 나중에 Google Cloud Vision 코드 삽입)
    public ExpenseDto.OcrResponse analyzeReceipt(MultipartFile image) {
        // TODO: 구글 클라우드 Vision API 연동
        return ExpenseDto.OcrResponse.builder()
                .parsedTitle("쏨분씨푸드")
                .parsedTotalAmount(new BigDecimal("55000"))
                .parsedItems(List.of(
                        new ExpenseDto.ParsedItem("푸팟퐁커리", new BigDecimal("30000")),
                        new ExpenseDto.ParsedItem("창 맥주", new BigDecimal("10000"))
                ))
                .build();
    }

    @Transactional
    public void createExpense(Long userID, Long tripId, ExpenseDto.CreateRequest request) {

        checkMembership(tripId, userID);

        Expense expense = Expense.builder()
                .tripId(tripId)
                .title(request.getTitle())
                .totalAmount(request.getTotalAmount())
                .expenseType(request.getExpenseType())
                .paymentTime(request.getPaymentTime())
                .currency(request.getCurrency())
                .exchangeRate(request.getExchangeRate())
                .receiptImageUrl(request.getReceiptImageUrl())
                .payerUserId(request.getPayerUserId())
                .build();
        expenseRepository.save(expense);

        Map<Long, BigDecimal> userOwedMap = new HashMap<>();

        for (ExpenseDto.ItemRequest itemReq : request.getItems()) {
            ExpenseItem expenseItem = ExpenseItem.builder()
                    .expense(expense)
                    .itemName(itemReq.getItemName())
                    .price(itemReq.getPrice())
                    .build();
            expense.getItems().add(expenseItem);

            List<Long> participants = itemReq.getParticipantUserIds();

            if (participants == null || participants.isEmpty()) {
                Trip trip = tripRepository.findById(tripId)
                        .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_FOUND));
                participants = tripMemberRepository.findAllByTrip(trip).stream()
                        .map(tm -> tm.getUser().getId())
                        .collect(Collectors.toList());
            }

            if (participants != null && !participants.isEmpty()) {
                BigDecimal splitAmount = itemReq.getPrice().divide(
                        new BigDecimal(participants.size()), 0, RoundingMode.DOWN);

                for (Long userId : participants) {
                    userOwedMap.put(userId, userOwedMap.getOrDefault(userId, BigDecimal.ZERO).add(splitAmount));

                    ExpenseItemParticipant participantEntity = ExpenseItemParticipant.builder()
                            .expenseItem(expenseItem)
                            .userId(userId)
                            .build();
                    expenseItem.getParticipants().add(participantEntity);
                }
            }
        }

        userOwedMap.putIfAbsent(request.getPayerUserId(), BigDecimal.ZERO);

        for (Map.Entry<Long, BigDecimal> entry : userOwedMap.entrySet()) {
            Long userId = entry.getKey();
            BigDecimal amountOwed = entry.getValue();

            BigDecimal amountPaid = userId.equals(request.getPayerUserId()) ? request.getTotalAmount() : BigDecimal.ZERO;

            ExpenseMember member = ExpenseMember.builder()
                    .expense(expense)
                    .userId(userId)
                    .amountPaid(amountPaid)
                    .amountOwed(amountOwed)
                    .build();
            expenseMemberRepository.save(member);
        }
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto.DetailResponse> getExpensesByTrip(Long userId, Long tripId) {
        checkMembership(tripId, userId);
        List<Expense> expenses = expenseRepository.findAllByTripIdOrderByPaymentTimeDesc(tripId);

        Set<Long> userIds = new HashSet<>();
        for (Expense expense : expenses) {
            userIds.add(expense.getPayerUserId());
            for (ExpenseItem item : expense.getItems()) {
                for (ExpenseItemParticipant participant : item.getParticipants()) {
                    userIds.add(participant.getUserId());
                }
            }
        }

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return expenses.stream().map(expense -> {

            User payer = userMap.get(expense.getPayerUserId());
            if (payer == null) throw new CustomException(ErrorCode.USER_NOT_FOUND);

            List<ExpenseDto.DetailItem> detailItems = expense.getItems().stream().map(item -> {

                List<ExpenseDto.PayerInfo> participants = item.getParticipants().stream()
                        .map(participant -> {
                            User user = userMap.get(participant.getUserId());
                            if (user == null) throw new CustomException(ErrorCode.USER_NOT_FOUND);
                            return new ExpenseDto.PayerInfo(user.getId(), user.getNickname());
                        })
                        .collect(Collectors.toList());

                return ExpenseDto.DetailItem.builder()
                        .itemName(item.getItemName())
                        .price(item.getPrice())
                        .participants(participants)
                        .build();
            }).collect(Collectors.toList());

            return ExpenseDto.DetailResponse.builder()
                    .expenseId(expense.getId())
                    .title(expense.getTitle())
                    .totalAmount(expense.getTotalAmount())
                    .payer(new ExpenseDto.PayerInfo(payer.getId(), payer.getNickname()))
                    .paymentTime(expense.getPaymentTime())
                    .expenseType(expense.getExpenseType())
                    .itemCount(expense.getItems().size())
                    .receiptImageUrl(expense.getReceiptImageUrl())
                    .items(detailItems)
                    .build();
        }).collect(Collectors.toList());
    }
    private void checkMembership(Long tripId, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!tripMemberRepository.existsByTripAndUser(trip, user)) {
            throw new CustomException(ErrorCode.NOT_TRIP_MEMBER);
        }
    }
}
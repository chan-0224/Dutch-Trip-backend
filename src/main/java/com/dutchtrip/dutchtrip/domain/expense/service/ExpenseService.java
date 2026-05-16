package com.dutchtrip.dutchtrip.domain.expense.service;

import com.dutchtrip.dutchtrip.domain.expense.dto.ExpenseDto;
import com.dutchtrip.dutchtrip.domain.expense.entity.Expense;
import com.dutchtrip.dutchtrip.domain.expense.entity.ExpenseItem;
import com.dutchtrip.dutchtrip.domain.expense.entity.ExpenseItemParticipant;
import com.dutchtrip.dutchtrip.domain.expense.entity.ExpenseMember;
import com.dutchtrip.dutchtrip.domain.expense.repository.ExpenseMemberRepository;
import com.dutchtrip.dutchtrip.domain.expense.repository.ExpenseRepository;
import com.dutchtrip.dutchtrip.domain.user.entity.User;
import com.dutchtrip.dutchtrip.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseMemberRepository expenseMemberRepository;
    private final UserRepository userRepository;
    private final EntityManager em;

    //OCR 분석 (임시 -나중에 Google Cloud Vision 코드 삽입)
    public ExpenseDto.OcrResponse analyzeReceipt(MultipartFile image) {
        // TODO: 구글 클라우드 Vision API 연동
        // 지금은 테스트를 위해 더미 데이터를 반환
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
    public void createExpense(Long tripId, ExpenseDto.CreateRequest request) {

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
                participants = em.createQuery(
                                "SELECT tm.user.id FROM TripMember tm WHERE tm.trip.id = :tripId", Long.class)
                        .setParameter("tripId", tripId)
                        .getResultList();
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

//    @Transactional(readOnly = true)
//    public List<ExpenseDto.SummaryResponse> getExpensesByTrip(Long tripId) {
//        List<Expense> expenses = expenseRepository.findAllByTripIdOrderByPaymentTimeDesc(tripId);
//
//        return expenses.stream().map(expense -> {
//
//            User payer = userRepository.findById(expense.getPayerUserId()).orElseThrow();
//
//            return ExpenseDto.SummaryResponse.builder()
//                    .expenseId(expense.getId())
//                    .title(expense.getTitle())
//                    .totalAmount(expense.getTotalAmount())
//                    .payer(new ExpenseDto.PayerInfo(payer.getId(), payer.getNickname()))
//                    .paymentTime(expense.getPaymentTime())
//                    .expenseType(expense.getExpenseType())
//                    .itemCount(expense.getItems().size())
//                    .build();
//        }).collect(Collectors.toList());
//    }

    @Transactional(readOnly = true)
    public List<ExpenseDto.DetailResponse> getExpensesByTrip(Long tripId) {
        List<Expense> expenses = expenseRepository.findAllByTripIdOrderByPaymentTimeDesc(tripId);

        return expenses.stream().map(expense -> {

            User payer = userRepository.findById(expense.getPayerUserId())
                    .orElseThrow(() -> new IllegalArgumentException("결제자 정보를 찾을 수 없습니다."));

            List<ExpenseDto.DetailItem> detailItems = expense.getItems().stream().map(item -> {

                List<ExpenseDto.PayerInfo> participants = item.getParticipants().stream()
                        .map(participant -> {
                            User user = userRepository.findById(participant.getUserId())
                                    .orElseThrow(() -> new IllegalArgumentException("참여자 유저 정보를 찾을 수 없습니다."));
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
}
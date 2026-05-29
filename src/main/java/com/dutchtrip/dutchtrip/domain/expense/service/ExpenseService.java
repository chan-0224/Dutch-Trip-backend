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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import com.dutchtrip.dutchtrip.global.exception.CustomException;
import com.dutchtrip.dutchtrip.global.exception.ErrorCode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseMemberRepository expenseMemberRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    @Transactional(readOnly = true)
    public ExpenseDto.OcrResponse analyzeReceipt(Long userId, Long tripId, MultipartFile image) {
        checkMembership(tripId, userId);

        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String mimeType = image.getContentType();

            String prompt = "이 영수증 이미지를 분석해서 다음 형식의 순수 JSON 데이터만 반환해줘. 마크다운(` ```json `)이나 다른 설명은 절대 넣지 마.\n" +
                    "형식: {\"parsed_title\": \"가게이름\", \"parsed_total_amount\": 총액숫자, \"parsed_items\": [{\"item_name\": \"메뉴명\", \"price\": 가격숫자}]}";

            Map<String, Object> inlineData = Map.of(
                    "mimeType", mimeType != null ? mimeType : "image/jpeg",
                    "data", base64Image
            );

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt),
                                    Map.of("inlineData", inlineData)
                            ))
                    )
            );

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String response = restTemplate.postForObject(url, request, String.class);

            JsonNode rootNode = objectMapper.readTree(response);
            String jsonText = rootNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            jsonText = jsonText.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(jsonText, ExpenseDto.OcrResponse.class);

        } catch (Exception e) {
            log.error("Gemini 3.1 Flash 영수증 OCR 분석 또는 JSON 파싱 중 오류 발생. tripId: {}, userId: {}", tripId, userId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @CacheEvict(value = "settlements", key = "#tripId")
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

                BigDecimal totalSplit = splitAmount.multiply(new BigDecimal(participants.size()));
                BigDecimal remainder = itemReq.getPrice().subtract(totalSplit);

                for (Long userId : participants) {
                    userOwedMap.put(userId, userOwedMap.getOrDefault(userId, BigDecimal.ZERO).add(splitAmount));

                    ExpenseItemParticipant participantEntity = ExpenseItemParticipant.builder()
                            .expenseItem(expenseItem)
                            .userId(userId)
                            .build();
                    expenseItem.getParticipants().add(participantEntity);
                }

                if (remainder.compareTo(BigDecimal.ZERO) > 0) {
                    Long payerId = request.getPayerUserId();
                    userOwedMap.put(payerId, userOwedMap.getOrDefault(payerId, BigDecimal.ZERO).add(remainder));
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
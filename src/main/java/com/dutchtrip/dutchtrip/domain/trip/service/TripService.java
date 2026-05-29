package com.dutchtrip.dutchtrip.domain.trip.service;

import com.dutchtrip.dutchtrip.domain.expense.entity.Expense;
import com.dutchtrip.dutchtrip.domain.expense.entity.ExpenseMember;
import com.dutchtrip.dutchtrip.domain.expense.repository.ExpenseMemberRepository;
import com.dutchtrip.dutchtrip.domain.expense.repository.ExpenseRepository;
import com.dutchtrip.dutchtrip.domain.trip.dto.JoinTripRequest;
import com.dutchtrip.dutchtrip.domain.trip.dto.TripCreateRequest;
import com.dutchtrip.dutchtrip.domain.trip.dto.TripCreateResponse;
import com.dutchtrip.dutchtrip.domain.trip.dto.TripListResponse;
import com.dutchtrip.dutchtrip.domain.trip.dto.TripMemberResponse;
import com.dutchtrip.dutchtrip.domain.trip.dto.TripResponse;
import com.dutchtrip.dutchtrip.domain.trip.entity.Trip;
import com.dutchtrip.dutchtrip.domain.trip.entity.TripMember;
import com.dutchtrip.dutchtrip.domain.trip.entity.TripMemberRole;
import com.dutchtrip.dutchtrip.domain.trip.repository.TripMemberRepository;
import com.dutchtrip.dutchtrip.domain.trip.repository.TripRepository;
import com.dutchtrip.dutchtrip.domain.user.entity.User;
import com.dutchtrip.dutchtrip.domain.user.repository.UserRepository;
import com.dutchtrip.dutchtrip.global.exception.CustomException;
import com.dutchtrip.dutchtrip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseMemberRepository expenseMemberRepository;

    @Transactional
    public TripCreateResponse createTrip(Long userId, TripCreateRequest request) {
        User user = findUser(userId);

        Trip trip = Trip.builder()
                .creator(user)
                .title(request.getTitle())
                .nation(request.getNation())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .inviteCode(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase())
                .build();
        tripRepository.save(trip);

        tripMemberRepository.save(TripMember.builder()
                .trip(trip)
                .user(user)
                .role(TripMemberRole.OWNER)
                .build());

        if (request.getFixedCosts() != null) {
            for (TripCreateRequest.FixedCostRequest fc : request.getFixedCosts()) {
                Expense expense = Expense.builder()
                        .tripId(trip.getId())
                        .title(fc.getTitle())
                        .totalAmount(fc.getTotalAmount())
                        .expenseType("고정")
                        .payerUserId(user.getId())
                        .build();
                expenseRepository.save(expense);

                expenseMemberRepository.save(ExpenseMember.builder()
                        .expense(expense)
                        .userId(user.getId())
                        .amountPaid(fc.getTotalAmount())
                        .amountOwed(fc.getTotalAmount())
                        .build());
            }
        }

        return TripCreateResponse.from(trip);
    }

    public List<TripListResponse> getMyTrips(Long userId) {
        User user = findUser(userId);
        return tripMemberRepository.findAllByUser(user).stream()
                .map(member -> TripListResponse.from(
                        member.getTrip(),
                        tripMemberRepository.countByTrip(member.getTrip()),
                        member.getRole()))
                .toList();
    }

    public List<TripMemberResponse> getTripMembers(Long userId, Long tripId) {
        User user = findUser(userId);
        Trip trip = findTrip(tripId);
        if (!tripMemberRepository.existsByTripAndUser(trip, user)) {
            throw new CustomException(ErrorCode.NOT_TRIP_MEMBER);
        }
        return tripMemberRepository.findAllByTrip(trip).stream()
                .map(TripMemberResponse::from)
                .toList();
    }

    public TripResponse getTrip(Long userId, Long tripId) {
        User user = findUser(userId);
        Trip trip = findTrip(tripId);

        if (!tripMemberRepository.existsByTripAndUser(trip, user)) {
            throw new CustomException(ErrorCode.NOT_TRIP_MEMBER);
        }

        return TripResponse.from(trip);
    }

    @Transactional
    public TripResponse joinTrip(Long userId, JoinTripRequest request) {
        User user = findUser(userId);
        Trip trip = tripRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INVITE_CODE));

        if (tripMemberRepository.existsByTripAndUser(trip, user)) {
            throw new CustomException(ErrorCode.ALREADY_JOINED);
        }

        tripMemberRepository.save(TripMember.builder()
                .trip(trip)
                .user(user)
                .role(TripMemberRole.MEMBER)
                .build());

        List<Expense> fixedExpenses = expenseRepository.findAllByTripIdAndExpenseType(trip.getId(), "고정");
        if (!fixedExpenses.isEmpty()) {
            List<TripMember> allMembers = tripMemberRepository.findAllByTrip(trip);
            int memberCount = allMembers.size();

            for (Expense fixedExpense : fixedExpenses) {
                BigDecimal perPerson = fixedExpense.getTotalAmount()
                        .divide(new BigDecimal(memberCount), 0, RoundingMode.DOWN);

                expenseMemberRepository.deleteAll(expenseMemberRepository.findAllByExpense(fixedExpense));

                for (TripMember member : allMembers) {
                    BigDecimal paid = member.getUser().getId().equals(fixedExpense.getPayerUserId())
                            ? fixedExpense.getTotalAmount() : BigDecimal.ZERO;
                    expenseMemberRepository.save(ExpenseMember.builder()
                            .expense(fixedExpense)
                            .userId(member.getUser().getId())
                            .amountPaid(paid)
                            .amountOwed(perPerson)
                            .build());
                }
            }
        }

        return TripResponse.from(trip);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Trip findTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_FOUND));
    }
}

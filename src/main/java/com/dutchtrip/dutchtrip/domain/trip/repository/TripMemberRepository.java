package com.dutchtrip.dutchtrip.domain.trip.repository;

import com.dutchtrip.dutchtrip.domain.trip.entity.Trip;
import com.dutchtrip.dutchtrip.domain.trip.entity.TripMember;
import com.dutchtrip.dutchtrip.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripMemberRepository extends JpaRepository<TripMember, Long> {
    List<TripMember> findAllByUser(User user);
    List<TripMember> findAllByTrip(Trip trip);
    boolean existsByTripAndUser(Trip trip, User user);
    long countByTrip(Trip trip);
}

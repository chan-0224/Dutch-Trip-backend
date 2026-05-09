package com.dutchtrip.dutchtrip.domain.trip.repository;

import com.dutchtrip.dutchtrip.domain.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findByInviteCode(String inviteCode);
}

package org.dispatchsystem.ride.repository;

import org.dispatchsystem.common.events.RideDispatchEvent;
import org.dispatchsystem.common.events.domains.ReasonCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RideDispatchEventRepository extends JpaRepository<RideDispatchEvent, Long> {
    List<RideDispatchEvent> findByRide_IdOrderByCreatedAtAsc(Long rideId);
    List<RideDispatchEvent>findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}

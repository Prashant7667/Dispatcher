package org.dispatchsystem.ride.repository;

import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByUserEmail(String email);

    List<Ride> findByDriverEmail(String email);

    List<Ride>findByStatusAndScheduledStartLessThanEqual(RideStatus status, LocalDateTime scheduledAt);

}

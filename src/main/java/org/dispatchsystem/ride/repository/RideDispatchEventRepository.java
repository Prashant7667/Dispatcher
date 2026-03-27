package org.dispatchsystem.ride.repository;

import org.dispatchsystem.common.events.RideDispatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RideDispatchEventRepository extends JpaRepository<RideDispatchEvent, Long> {
    java.util.List<RideDispatchEvent> findByRide_IdOrderByCreatedAtAsc(Long rideId);
}

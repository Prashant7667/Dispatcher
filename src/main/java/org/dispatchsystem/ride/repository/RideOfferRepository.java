package org.dispatchsystem.ride.repository;

import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RideOfferRepository extends JpaRepository<RideOffer,Long> {
    Optional<RideOffer> findTopByDriver_EmailAndRide_IdOrderBySentAtDesc(String driverEmail, Long rideId);
}

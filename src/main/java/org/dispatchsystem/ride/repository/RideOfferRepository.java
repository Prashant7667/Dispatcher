package org.dispatchsystem.ride.repository;

import org.dispatchsystem.ride.domain.RideOffer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideOfferRepository extends JpaRepository<RideOffer,Long> {
}

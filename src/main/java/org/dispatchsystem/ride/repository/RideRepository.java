package org.dispatchsystem.ride.repository;

import org.dispatchsystem.ride.domain.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByUserEmail(String email);

    List<Ride> findByDriverEmail(String email);

}

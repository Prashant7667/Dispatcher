package org.dispatchsystem.driver.repository;

import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver,Long> {
    public Optional<Driver> findByEmail(String email);
    public List<Driver> findByAvailabilityStatus(AvailabilityStatus status);
    @Modifying
    @Query("""
        update Driver d
        set d.availabilityStatus = :reserved
        where d.id = :driverId and d.availabilityStatus = :available
        """)
    int updateDriver(Long driverId, AvailabilityStatus available, AvailabilityStatus reserved);
}

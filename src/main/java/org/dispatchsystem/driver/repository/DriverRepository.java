package org.dispatchsystem.driver.repository;

import org.dispatchsystem.driver.domain.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver,Long> {
    public Optional<Driver> findByEmail(String email);
}

package org.dispatchsystem.driver.repository;

import org.dispatchsystem.driver.domain.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver,Long> {
}

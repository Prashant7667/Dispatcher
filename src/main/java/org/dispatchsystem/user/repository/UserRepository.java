package org.dispatchsystem.user.repository;
import org.dispatchsystem.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
}

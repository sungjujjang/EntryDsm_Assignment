package com.sungjujjang.entry.Application;

import com.sungjujjang.entry.Auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findById(Long id);

    Optional<Application> findByUser(User user);

    Boolean existsByUser(User user);
}

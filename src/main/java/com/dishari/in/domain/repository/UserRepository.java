package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    boolean existsByEmail(String email) ;

    Optional<User> findByEmail(String email) ;
}
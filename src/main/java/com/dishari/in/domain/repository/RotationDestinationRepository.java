package com.dishari.in.domain.repository;

import com.dishari.in.domain.entity.RotationDestination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RotationDestinationRepository extends JpaRepository<RotationDestination, UUID> {
}
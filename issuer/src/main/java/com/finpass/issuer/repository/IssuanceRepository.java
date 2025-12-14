package com.finpass.issuer.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finpass.issuer.entity.IssuanceEntity;

public interface IssuanceRepository extends JpaRepository<IssuanceEntity, UUID> {
}

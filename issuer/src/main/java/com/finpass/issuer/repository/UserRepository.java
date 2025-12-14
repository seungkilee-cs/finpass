package com.finpass.issuer.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finpass.issuer.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
	Optional<UserEntity> findByDid(String did);
}

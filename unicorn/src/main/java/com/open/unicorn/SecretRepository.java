package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SecretRepository extends JpaRepository<Secret, Long> {

    @Query("SELECT s FROM Secret s WHERE s.userId = :userId AND s.isActive = true ORDER BY s.updatedAt DESC")
    List<Secret> findByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT s FROM Secret s WHERE s.userId = :userId AND s.name = :name AND s.isActive = true")
    Optional<Secret> findByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);

    @Query("SELECT s FROM Secret s WHERE s.userId = :userId AND s.id = :id AND s.isActive = true")
    Optional<Secret> findByUserIdAndId(@Param("userId") Long userId, @Param("id") Long id);

    @Query("SELECT COUNT(s) FROM Secret s WHERE s.userId = :userId AND s.isActive = true")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Secret s WHERE s.userId = :userId AND s.expiresAt IS NOT NULL AND s.expiresAt <= :expiryDate AND s.isActive = true")
    List<Secret> findExpiredSecrets(@Param("userId") Long userId, @Param("expiryDate") java.time.LocalDateTime expiryDate);
} 
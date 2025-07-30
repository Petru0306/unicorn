package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QueueRepository extends JpaRepository<Queue, Long> {
    
    // Find all queues for a specific user
    List<Queue> findByUserId(Long userId);
    
    // Find a queue by name for a specific user
    Optional<Queue> findByUserIdAndQueueName(Long userId, String queueName);
    
    // Check if a queue name exists for a user
    boolean existsByUserIdAndQueueName(Long userId, String queueName);
    
    // Find queue by ID and user ID (for security)
    Optional<Queue> findByIdAndUserId(Long id, Long userId);
} 
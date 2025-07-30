package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoSQLTableRepository extends JpaRepository<NoSQLTable, Long> {
    
    // Find all tables for a specific user
    List<NoSQLTable> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Find a table by name and user ID (for uniqueness validation)
    Optional<NoSQLTable> findByNameAndUserId(String name, Long userId);
    
    // Check if table exists for user
    boolean existsByNameAndUserId(String name, Long userId);
    
    // Find table by ID and user ID (for security)
    Optional<NoSQLTable> findByIdAndUserId(Long id, Long userId);
    
    // Count tables for a user
    long countByUserId(Long userId);
} 
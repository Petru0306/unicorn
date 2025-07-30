package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoSQLIndexRepository extends JpaRepository<NoSQLIndex, Long> {
    
    // Find all indexes for a specific table
    List<NoSQLIndex> findByTableIdAndIsActiveTrueOrderByCreatedAtDesc(Long tableId);
    
    // Find all indexes for a user
    List<NoSQLIndex> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Find index by name and table ID
    Optional<NoSQLIndex> findByIndexNameAndTableId(String indexName, Long tableId);
    
    // Check if index exists by name and table ID
    boolean existsByIndexNameAndTableId(String indexName, Long tableId);
    
    // Find indexes by field name and table ID
    List<NoSQLIndex> findByFieldNameAndTableIdAndIsActiveTrue(String fieldName, Long tableId);
    
    // Count indexes for a table
    long countByTableIdAndIsActiveTrue(Long tableId);
    
    // Delete all indexes for a table
    void deleteByTableId(Long tableId);
    
    // Find indexes by user and table
    List<NoSQLIndex> findByUserIdAndTableIdAndIsActiveTrue(Long userId, Long tableId);
} 
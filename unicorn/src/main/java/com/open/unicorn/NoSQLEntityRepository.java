package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoSQLEntityRepository extends JpaRepository<NoSQLEntity, Long> {
    
    // Find all entities for a specific table
    List<NoSQLEntity> findByTableIdOrderByCreatedAtDesc(Long tableId);
    
    // Find entity by table ID and primary key value
    Optional<NoSQLEntity> findByTableIdAndPrimaryKeyValue(Long tableId, String primaryKeyValue);
    
    // Check if entity exists by table ID and primary key value
    boolean existsByTableIdAndPrimaryKeyValue(Long tableId, String primaryKeyValue);
    
    // Count entities in a table
    long countByTableId(Long tableId);
    
    // Find entities with pagination
    @Query("SELECT e FROM NoSQLEntity e WHERE e.tableId = :tableId ORDER BY e.createdAt DESC")
    Page<NoSQLEntity> findByTableIdWithPagination(@Param("tableId") Long tableId, org.springframework.data.domain.Pageable pageable);
    
    // Query entities by JSON field (using text search for H2)
    @Query(value = "SELECT * FROM nosql_entities WHERE table_id = :tableId AND document_data LIKE :fieldValue", nativeQuery = true)
    List<NoSQLEntity> findByTableIdAndFieldValue(@Param("tableId") Long tableId, @Param("fieldValue") String fieldValue);
    
    // Query entities by specific JSON field and value (simplified for H2)
    @Query(value = "SELECT * FROM nosql_entities WHERE table_id = :tableId AND document_data LIKE :searchPattern", nativeQuery = true)
    List<NoSQLEntity> findByTableIdAndJsonField(@Param("tableId") Long tableId, @Param("fieldName") String fieldName, @Param("fieldValue") String fieldValue);
    
    // Delete all entities for a table
    void deleteByTableId(Long tableId);
} 
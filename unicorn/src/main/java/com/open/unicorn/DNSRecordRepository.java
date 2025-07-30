package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DNSRecordRepository extends JpaRepository<DNSRecord, Long> {
    
    // Find all records for a specific zone
    List<DNSRecord> findByZoneIdOrderByTypeAsc(Long zoneId);
    
    // Find records by type for a specific zone
    List<DNSRecord> findByZoneIdAndTypeOrderByNameAsc(Long zoneId, String type);
    
    // Find record by name and type for a specific zone
    DNSRecord findByZoneIdAndNameAndType(Long zoneId, String name, String type);
    
    // Check if record exists by name and type for a specific zone
    boolean existsByZoneIdAndNameAndType(Long zoneId, String name, String type);
    
    // Find records by zone ID and name (for CNAME conflicts)
    List<DNSRecord> findByZoneIdAndName(Long zoneId, String name);
    
    // Custom query to get records with zone information
    @Query("SELECT r FROM DNSRecord r WHERE r.zoneId = :zoneId ORDER BY r.type, r.name")
    List<DNSRecord> findRecordsByZoneIdOrdered(@Param("zoneId") Long zoneId);
} 
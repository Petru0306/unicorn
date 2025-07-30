package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DNSZoneRepository extends JpaRepository<DNSZone, Long> {
    
    // Find all zones for a specific user
    List<DNSZone> findByUserId(Long userId);
    
    // Find zone by name for a specific user
    DNSZone findByUserIdAndZoneName(Long userId, String zoneName);
    
    // Check if zone name exists for a user
    boolean existsByUserIdAndZoneName(Long userId, String zoneName);
} 
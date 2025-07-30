package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DatabaseInstanceRepository extends JpaRepository<DatabaseInstance, Long> {
    
    @Query("SELECT d FROM DatabaseInstance d WHERE d.user.id = :userId")
    List<DatabaseInstance> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT d FROM DatabaseInstance d WHERE d.user.id = :userId AND d.dbName = :dbName")
    DatabaseInstance findByUserIdAndDbName(@Param("userId") Long userId, @Param("dbName") String dbName);
    
    @Query("SELECT COUNT(d) FROM DatabaseInstance d WHERE d.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
} 
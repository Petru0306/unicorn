package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AIExecutionRepository extends JpaRepository<AIExecution, Long> {
    
    List<AIExecution> findByUserOrderByExecutedAtDesc(User user);
    
    List<AIExecution> findByAiFunctionOrderByExecutedAtDesc(AIFunction aiFunction);
    
    @Query("SELECT COUNT(ae) FROM AIExecution ae WHERE ae.user = :user")
    Long countByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(ae) FROM AIExecution ae WHERE ae.aiFunction = :aiFunction")
    Long countByAiFunction(@Param("aiFunction") AIFunction aiFunction);
    
    @Query("SELECT COUNT(ae) FROM AIExecution ae WHERE ae.user = :user AND CAST(ae.executedAt AS DATE) = CURRENT_DATE")
    Long countTodayByUser(@Param("user") User user);
} 
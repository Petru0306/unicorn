package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LambdaExecutionRepository extends JpaRepository<LambdaExecution, Long> {
    
    List<LambdaExecution> findByLambda(Lambda lambda);
    
    List<LambdaExecution> findByLambdaOrderByTimestampDesc(Lambda lambda);
} 
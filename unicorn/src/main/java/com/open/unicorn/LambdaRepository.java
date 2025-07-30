package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LambdaRepository extends JpaRepository<Lambda, Long> {
    
    List<Lambda> findByUser(User user);
    
    Optional<Lambda> findByUserAndId(User user, Long id);
    
    void deleteByUserAndId(User user, Long id);
} 
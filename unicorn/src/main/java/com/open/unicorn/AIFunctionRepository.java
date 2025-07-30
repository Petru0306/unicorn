package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AIFunctionRepository extends JpaRepository<AIFunction, Long> {
    
    List<AIFunction> findByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT COUNT(af) FROM AIFunction af WHERE af.user = :user")
    Long countByUser(@Param("user") User user);
    
    @Query("SELECT af FROM AIFunction af WHERE af.user = :user AND af.id = :functionId")
    AIFunction findByUserAndId(@Param("user") User user, @Param("functionId") Long functionId);
} 
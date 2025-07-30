package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface IAMRoleRepository extends JpaRepository<IAMRole, Long> {
    
    Optional<IAMRole> findByName(String name);
    
    List<IAMRole> findByCreatedBy(User createdBy);
    
    @Query("SELECT r FROM IAMRole r WHERE r.name LIKE %:searchTerm% OR r.description LIKE %:searchTerm%")
    List<IAMRole> searchRoles(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT r FROM IAMRole r WHERE r.createdBy = :user AND (r.name LIKE %:searchTerm% OR r.description LIKE %:searchTerm%)")
    List<IAMRole> searchRolesByUser(@Param("user") User user, @Param("searchTerm") String searchTerm);
    
    boolean existsByName(String name);
} 
package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    
    Optional<UserRole> findByUser(User user);
    
    List<UserRole> findByRole(IAMRole role);
    
    @Query("SELECT ur FROM UserRole ur WHERE ur.role.createdBy = :owner")
    List<UserRole> findByRoleOwner(@Param("owner") User owner);
    
    @Query("SELECT ur FROM UserRole ur WHERE ur.role.createdBy = :owner AND ur.user.email LIKE %:searchTerm%")
    List<UserRole> findByRoleOwnerAndUserSearch(@Param("owner") User owner, @Param("searchTerm") String searchTerm);
    
    boolean existsByUser(User user);
    
    void deleteByUser(User user);
} 
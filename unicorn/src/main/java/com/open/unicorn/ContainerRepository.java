package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContainerRepository extends JpaRepository<Container, Long> {
    List<Container> findByOwnerEmail(String ownerEmail);
    Container findByInstanceId(String instanceId);
    List<Container> findByOwnerEmailAndStatus(String ownerEmail, String status);
    boolean existsByInstanceId(String instanceId);
} 
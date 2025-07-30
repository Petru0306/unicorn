package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BucketRepository extends JpaRepository<Bucket, Long> {
    List<Bucket> findByOwnerEmail(String ownerEmail);
    Bucket findByName(String name);
    boolean existsByName(String name);
} 
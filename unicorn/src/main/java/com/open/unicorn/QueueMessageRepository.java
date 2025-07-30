package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueMessageRepository extends JpaRepository<QueueMessage, Long> {
    
    // Find all messages in a queue
    List<QueueMessage> findByQueueIdOrderByCreatedAtAsc(Long queueId);
    
    // Find all non-deleted messages in a queue
    @Query("SELECT m FROM QueueMessage m WHERE m.queue.id = :queueId AND m.status != 'DELETED' ORDER BY m.createdAt ASC")
    List<QueueMessage> findNonDeletedMessagesByQueueId(@Param("queueId") Long queueId);
    
    // Find visible messages in a queue (for receiving)
    @Query("SELECT m FROM QueueMessage m WHERE m.queue.id = :queueId AND m.status = 'VISIBLE' ORDER BY m.createdAt ASC")
    List<QueueMessage> findVisibleMessagesByQueueId(@Param("queueId") Long queueId);
    
    // Find messages that have expired visibility timeout
    @Query("SELECT m FROM QueueMessage m WHERE m.status = 'INVISIBLE' AND m.visibilityExpiry < :now")
    List<QueueMessage> findExpiredInvisibleMessages(@Param("now") LocalDateTime now);
    
    // Find message by ID and queue ID (for security)
    Optional<QueueMessage> findByIdAndQueueId(Long id, Long queueId);
    
    // Delete messages older than retention time
    @Modifying
    @Query("DELETE FROM QueueMessage m WHERE m.queue.id = :queueId AND m.createdAt < :retentionCutoff")
    void deleteExpiredMessages(@Param("queueId") Long queueId, @Param("retentionCutoff") LocalDateTime retentionCutoff);
    
    // Count messages in a queue
    long countByQueueId(Long queueId);
    
    // Count non-deleted messages in a queue
    @Query("SELECT COUNT(m) FROM QueueMessage m WHERE m.queue.id = :queueId AND m.status != 'DELETED'")
    long countNonDeletedMessagesByQueueId(@Param("queueId") Long queueId);
    
    // Count visible messages in a queue
    @Query("SELECT COUNT(m) FROM QueueMessage m WHERE m.queue.id = :queueId AND m.status = 'VISIBLE'")
    long countVisibleMessagesByQueueId(@Param("queueId") Long queueId);
} 
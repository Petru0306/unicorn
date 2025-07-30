package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/uws-sqs")
public class UWSSQSController {

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private QueueMessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    // Create a new queue
    @PostMapping("/queues")
    public ResponseEntity<?> createQueue(@RequestBody CreateQueueRequest request, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            // Validate input
            if (request.getQueueName() == null || request.getQueueName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Queue name is required"));
            }

            // Check if queue name already exists for this user
            if (queueRepository.existsByUserIdAndQueueName(user.getId(), request.getQueueName())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Queue name already exists"));
            }

            // Create queue
            Queue queue = new Queue(user, request.getQueueName());
            
            // Set optional parameters
            if (request.getVisibilityTimeout() != null) {
                queue.setVisibilityTimeout(request.getVisibilityTimeout());
            }
            if (request.getRetentionTime() != null) {
                queue.setRetentionTime(request.getRetentionTime());
            }
            
            Queue savedQueue = queueRepository.save(queue);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Queue created successfully");
            response.put("queue", Map.of(
                "id", savedQueue.getId(),
                "queueName", savedQueue.getQueueName(),
                "visibilityTimeout", savedQueue.getVisibilityTimeout(),
                "retentionTime", savedQueue.getRetentionTime(),
                "createdAt", savedQueue.getCreatedAt()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create queue: " + e.getMessage()));
        }
    }

    // List all queues for the authenticated user
    @GetMapping("/queues")
    public ResponseEntity<?> listQueues(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            List<Queue> queues = queueRepository.findByUserId(user.getId());
            List<Map<String, Object>> response = new ArrayList<>();

            for (Queue queue : queues) {
                long messageCount = messageRepository.countNonDeletedMessagesByQueueId(queue.getId());
                long visibleMessageCount = messageRepository.countVisibleMessagesByQueueId(queue.getId());
                
                Map<String, Object> queueMap = new HashMap<>();
                queueMap.put("id", queue.getId());
                queueMap.put("queueName", queue.getQueueName());
                queueMap.put("visibilityTimeout", queue.getVisibilityTimeout());
                queueMap.put("retentionTime", queue.getRetentionTime());
                queueMap.put("createdAt", queue.getCreatedAt());
                queueMap.put("totalMessages", messageCount);
                queueMap.put("visibleMessages", visibleMessageCount);
                response.add(queueMap);
            }

            return ResponseEntity.ok(Map.of("queues", response));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to list queues: " + e.getMessage()));
        }
    }

    // Delete a queue
    @DeleteMapping("/queues/{queueId}")
    public ResponseEntity<?> deleteQueue(@PathVariable Long queueId, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            Optional<Queue> queueOpt = queueRepository.findByIdAndUserId(queueId, user.getId());
            if (queueOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Queue queue = queueOpt.get();
            
            // Delete all messages in the queue first
            List<QueueMessage> messages = messageRepository.findByQueueIdOrderByCreatedAtAsc(queueId);
            messageRepository.deleteAll(messages);
            
            // Delete the queue
            queueRepository.delete(queue);

            return ResponseEntity.ok(Map.of("message", "Queue deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete queue: " + e.getMessage()));
        }
    }

    // Add a message to a queue
    @PostMapping("/queues/{queueId}/messages")
    public ResponseEntity<?> addMessage(@PathVariable Long queueId, @RequestBody AddMessageRequest request, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            Optional<Queue> queueOpt = queueRepository.findByIdAndUserId(queueId, user.getId());
            if (queueOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Queue queue = queueOpt.get();

            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message content is required"));
            }

            // Create and save message
            QueueMessage message = new QueueMessage(queue, request.getContent());
            QueueMessage savedMessage = messageRepository.save(message);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Message added successfully");
            response.put("messageId", savedMessage.getId());
            response.put("content", savedMessage.getContent());
            response.put("createdAt", savedMessage.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to add message: " + e.getMessage()));
        }
    }

    // View or receive messages from a queue
    @GetMapping("/queues/{queueId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long queueId, 
                                       @RequestParam(defaultValue = "false") boolean receive,
                                       @RequestParam(defaultValue = "10") int maxMessages,
                                       Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            Optional<Queue> queueOpt = queueRepository.findByIdAndUserId(queueId, user.getId());
            if (queueOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Queue queue = queueOpt.get();
            List<QueueMessage> messages;

            System.out.println("=== SQS DEBUG ===");
            System.out.println("Queue ID: " + queueId);
            System.out.println("Receive mode: " + receive);
            System.out.println("Max messages: " + maxMessages);

            if (receive) {
                // Get visible messages and make them invisible
                System.out.println("RECEIVE MODE: Getting visible messages and making them invisible");
                messages = messageRepository.findVisibleMessagesByQueueId(queueId);
                System.out.println("Found " + messages.size() + " visible messages");
                
                messages = messages.stream().limit(maxMessages).collect(Collectors.toList());
                
                // Make messages invisible for visibility timeout
                for (QueueMessage message : messages) {
                    System.out.println("Making message " + message.getId() + " invisible for " + queue.getVisibilityTimeout() + " seconds");
                    message.makeInvisible(queue.getVisibilityTimeout());
                    messageRepository.save(message);
                }
            } else {
                // Just peek at messages - show only visible messages (not deleted, not currently invisible)
                System.out.println("PEEK MODE: Getting only visible messages");
                messages = messageRepository.findVisibleMessagesByQueueId(queueId);
                System.out.println("Found " + messages.size() + " visible messages for peek");
            }

            List<Map<String, Object>> response = new ArrayList<>();
            for (QueueMessage message : messages) {
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("id", message.getId());
                messageMap.put("content", message.getContent());
                messageMap.put("createdAt", message.getCreatedAt());
                messageMap.put("status", message.getStatus());
                messageMap.put("isVisible", message.isVisible());
                messageMap.put("isCurrentlyInvisible", message.isCurrentlyInvisible());
                messageMap.put("hasExpiredVisibility", message.hasExpiredVisibility());
                if (message.getVisibilityExpiry() != null) {
                    messageMap.put("visibilityExpiry", message.getVisibilityExpiry());
                }
                response.add(messageMap);
                System.out.println("Returning message " + message.getId() + " with status: " + message.getStatus() + ", visible: " + message.isVisible());
            }

            System.out.println("Total messages returned: " + response.size());
            System.out.println("=== END SQS DEBUG ===");

            return ResponseEntity.ok(Map.of("messages", response));
        } catch (Exception e) {
            System.err.println("Error in getMessages: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get messages: " + e.getMessage()));
        }
    }

    // Delete a message from a queue
    @DeleteMapping("/queues/{queueId}/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long queueId, @PathVariable Long messageId, Authentication authentication) {
        try {
            System.out.println("=== DELETE MESSAGE DEBUG ===");
            System.out.println("Queue ID: " + queueId);
            System.out.println("Message ID: " + messageId);
            
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                System.out.println("ERROR: User not found");
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            System.out.println("User: " + userEmail);

            // Verify queue belongs to user
            Optional<Queue> queueOpt = queueRepository.findByIdAndUserId(queueId, user.getId());
            if (queueOpt.isEmpty()) {
                System.out.println("ERROR: Queue not found or doesn't belong to user");
                return ResponseEntity.notFound().build();
            }

            System.out.println("Queue found: " + queueOpt.get().getQueueName());

            // Find and delete message
            Optional<QueueMessage> messageOpt = messageRepository.findByIdAndQueueId(messageId, queueId);
            if (messageOpt.isEmpty()) {
                System.out.println("ERROR: Message not found");
                return ResponseEntity.notFound().build();
            }

            QueueMessage message = messageOpt.get();
            System.out.println("Message found: " + message.getId() + " with status: " + message.getStatus());
            
            message.setStatus(QueueMessage.MessageStatus.DELETED);
            messageRepository.save(message);
            
            System.out.println("Message deleted successfully");
            System.out.println("=== END DELETE MESSAGE DEBUG ===");

            return ResponseEntity.ok(Map.of("message", "Message deleted successfully"));
        } catch (Exception e) {
            System.err.println("Error in deleteMessage: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete message: " + e.getMessage()));
        }
    }

    // Manual cleanup endpoint for testing
    @PostMapping("/queues/{queueId}/cleanup")
    public ResponseEntity<?> manualCleanup(@PathVariable Long queueId, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            Optional<Queue> queueOpt = queueRepository.findByIdAndUserId(queueId, user.getId());
            if (queueOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Force cleanup for this queue
            List<QueueMessage> expiredMessages = messageRepository.findExpiredInvisibleMessages(LocalDateTime.now());
            int restoredCount = 0;
            
            for (QueueMessage message : expiredMessages) {
                if (message.getQueue().getId().equals(queueId) && message.hasExpiredVisibility()) {
                    message.makeVisible();
                    messageRepository.save(message);
                    restoredCount++;
                }
            }

            return ResponseEntity.ok(Map.of(
                "message", "Cleanup completed",
                "restoredMessages", restoredCount
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to cleanup: " + e.getMessage()));
        }
    }

    // Scheduled task to clean up expired messages and restore visibility
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void cleanupExpiredMessages() {
        try {
            // Restore visibility for expired invisible messages
            List<QueueMessage> expiredMessages = messageRepository.findExpiredInvisibleMessages(LocalDateTime.now());
            for (QueueMessage message : expiredMessages) {
                if (message.hasExpiredVisibility()) {
                    message.makeVisible();
                    messageRepository.save(message);
                    System.out.println("Restored visibility for expired message: " + message.getId());
                }
            }

            // Delete messages older than retention time
            List<Queue> allQueues = queueRepository.findAll();
            for (Queue queue : allQueues) {
                LocalDateTime retentionCutoff = LocalDateTime.now().minusSeconds(queue.getRetentionTime());
                messageRepository.deleteExpiredMessages(queue.getId(), retentionCutoff);
            }
        } catch (Exception e) {
            System.err.println("Error in cleanup task: " + e.getMessage());
        }
    }

    // Request/Response classes
    public static class CreateQueueRequest {
        private String queueName;
        private Integer visibilityTimeout;
        private Integer retentionTime;

        // Getters and setters
        public String getQueueName() { return queueName; }
        public void setQueueName(String queueName) { this.queueName = queueName; }
        
        public Integer getVisibilityTimeout() { return visibilityTimeout; }
        public void setVisibilityTimeout(Integer visibilityTimeout) { this.visibilityTimeout = visibilityTimeout; }
        
        public Integer getRetentionTime() { return retentionTime; }
        public void setRetentionTime(Integer retentionTime) { this.retentionTime = retentionTime; }
    }

    public static class AddMessageRequest {
        private String content;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
} 
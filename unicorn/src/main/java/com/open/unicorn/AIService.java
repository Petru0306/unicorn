package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.open.unicorn.UserRepository;

@Service
public class AIService {

    @Autowired
    private AIFunctionRepository aiFunctionRepository;

    @Autowired
    private AIExecutionRepository aiExecutionRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    // Predefined AI models
    private static final Map<String, String> AI_MODELS = Map.of(
        "sentiment-analysis", "Sentiment Analysis",
        "image-labeling", "Image Labeling", 
        "code-explainer", "Code Explainer",
        "text-summarizer", "Text Summarizer",
        "language-detector", "Language Detector"
    );

    // Function types
    private static final List<String> FUNCTION_TYPES = Arrays.asList("text", "image", "code");

    // Resource limits
    private static final int MAX_FUNCTIONS_PER_USER = 10;
    private static final int MAX_EXECUTIONS_PER_DAY = 1000;

    public List<AIFunction> getUserFunctions(String userEmail) {
        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return aiFunctionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public AIFunction createFunction(String userEmail, String name, String type, String model) {
        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Check function limit
        Long functionCount = aiFunctionRepository.countByUser(user);
        if (functionCount >= MAX_FUNCTIONS_PER_USER) {
            throw new RuntimeException("Maximum number of AI functions reached (" + MAX_FUNCTIONS_PER_USER + ")");
        }

        // Validate type and model
        if (!FUNCTION_TYPES.contains(type)) {
            throw new RuntimeException("Invalid function type. Must be one of: " + FUNCTION_TYPES);
        }

        if (!AI_MODELS.containsKey(model)) {
            throw new RuntimeException("Invalid model. Must be one of: " + AI_MODELS.keySet());
        }

        AIFunction function = new AIFunction();
        function.setUser(user);
        function.setName(name);
        function.setType(type);
        function.setModel(model);
        function.setApiEndpoint("/api/ai/functions/" + UUID.randomUUID().toString() + "/invoke");

        return aiFunctionRepository.save(function);
    }

    public AIExecution invokeFunction(String userEmail, Long functionId, String input) {
        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        AIFunction function = aiFunctionRepository.findByUserAndId(user, functionId);
        if (function == null) {
            throw new RuntimeException("AI function not found");
        }

        // Check daily execution limit
        if (function.getCurrentExecutionsToday() >= function.getMaxExecutionsPerDay()) {
            throw new RuntimeException("Daily execution limit reached for this function");
        }

        // Reset daily counter if it's a new day
        LocalDateTime now = LocalDateTime.now();
        if (function.getLastResetDate().toLocalDate().isBefore(now.toLocalDate())) {
            function.setCurrentExecutionsToday(0);
            function.setLastResetDate(now);
        }

        // Increment execution counter
        function.setCurrentExecutionsToday(function.getCurrentExecutionsToday() + 1);
        aiFunctionRepository.save(function);

        // Create execution record
        AIExecution execution = new AIExecution();
        execution.setAiFunction(function);
        execution.setUser(user);
        execution.setInput(input);
        execution.setInputSizeBytes((long) input.getBytes().length);

        // Simulate AI processing time
        long startTime = System.currentTimeMillis();
        try {
            Thread.sleep(random.nextInt(500) + 200); // 200-700ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long executionTime = System.currentTimeMillis() - startTime;
        execution.setExecutionTimeMs(executionTime);

        // Generate mock AI output based on model type
        String output = generateMockAIOutput(function.getModel(), input, function.getType());
        execution.setOutput(output);
        execution.setOutputSizeBytes((long) output.getBytes().length);
        execution.setStatus("success");

        return aiExecutionRepository.save(execution);
    }

    public void deleteFunction(String userEmail, Long functionId) {
        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        AIFunction function = aiFunctionRepository.findByUserAndId(user, functionId);
        if (function == null) {
            throw new RuntimeException("AI function not found");
        }

        aiFunctionRepository.delete(function);
    }

    public List<AIExecution> getExecutionHistory(String userEmail, Long functionId) {
        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        AIFunction function = aiFunctionRepository.findByUserAndId(user, functionId);
        if (function == null) {
            throw new RuntimeException("AI function not found");
        }

        return aiExecutionRepository.findByAiFunctionOrderByExecutedAtDesc(function);
    }

    public Map<String, Object> getStatistics(String userEmail) {
        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFunctions", aiFunctionRepository.countByUser(user));
        stats.put("totalExecutions", aiExecutionRepository.countByUser(user));
        stats.put("todayExecutions", aiExecutionRepository.countTodayByUser(user));
        stats.put("maxFunctionsPerUser", MAX_FUNCTIONS_PER_USER);
        stats.put("maxExecutionsPerDay", MAX_EXECUTIONS_PER_DAY);

        return stats;
    }

    public Map<String, String> getAvailableModels() {
        return new HashMap<>(AI_MODELS);
    }

    public List<String> getAvailableTypes() {
        return new ArrayList<>(FUNCTION_TYPES);
    }

    private String generateMockAIOutput(String model, String input, String type) {
        try {
            switch (model) {
                case "sentiment-analysis":
                    return generateSentimentAnalysis(input);
                case "image-labeling":
                    return generateImageLabeling(input);
                case "code-explainer":
                    return generateCodeExplainer(input);
                case "text-summarizer":
                    return generateTextSummarizer(input);
                case "language-detector":
                    return generateLanguageDetector(input);
                default:
                    return "{\"result\": \"Unknown model type\", \"confidence\": 0.0}";
            }
        } catch (Exception e) {
            return "{\"error\": \"Failed to process input\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    private String generateSentimentAnalysis(String input) {
        String[] sentiments = {"positive", "negative", "neutral"};
        String sentiment = sentiments[random.nextInt(sentiments.length)];
        double confidence = 0.7 + random.nextDouble() * 0.3; // 0.7-1.0

        Map<String, Object> result = new HashMap<>();
        result.put("sentiment", sentiment);
        result.put("confidence", Math.round(confidence * 100.0) / 100.0);
        result.put("input_length", input.length());
        result.put("processed_at", LocalDateTime.now().toString());

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"Failed to generate sentiment analysis\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    private String generateImageLabeling(String input) {
        String[][] possibleLabels = {
            {"tree", "sky", "car", "building", "person"},
            {"cat", "dog", "bird", "flower", "mountain"},
            {"computer", "phone", "book", "chair", "table"},
            {"ocean", "beach", "sunset", "clouds", "grass"}
        };

        String[] labels = possibleLabels[random.nextInt(possibleLabels.length)];
        int numLabels = random.nextInt(3) + 2; // 2-4 labels

        List<Map<String, Object>> detectedObjects = new ArrayList<>();
        for (int i = 0; i < numLabels; i++) {
            Map<String, Object> obj = new HashMap<>();
            obj.put("label", labels[i]);
            obj.put("confidence", Math.round((0.6 + random.nextDouble() * 0.4) * 100.0) / 100.0);
            obj.put("bbox", Arrays.asList(
                random.nextDouble(), random.nextDouble(),
                random.nextDouble(), random.nextDouble()
            ));
            detectedObjects.add(obj);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("detected_objects", detectedObjects);
        result.put("total_objects", detectedObjects.size());
        result.put("processing_time_ms", random.nextInt(500) + 200);

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"Failed to generate image labeling\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    private String generateCodeExplainer(String input) {
        String[] explanations = {
            "This code implements a recursive function that calculates the Fibonacci sequence. It uses memoization to improve performance by storing previously calculated values.",
            "This is a sorting algorithm implementation using the quicksort method. It partitions the array around a pivot element and recursively sorts the subarrays.",
            "This function performs data validation by checking input parameters against predefined constraints and throwing appropriate exceptions for invalid data.",
            "This code creates a REST API endpoint that handles HTTP requests, processes JSON data, and returns structured responses with proper error handling.",
            "This is a database query function that uses prepared statements to prevent SQL injection attacks while efficiently retrieving data from the database."
        };

        String explanation = explanations[random.nextInt(explanations.length)];
        
        Map<String, Object> result = new HashMap<>();
        result.put("explanation", explanation);
        result.put("complexity", "O(n log n)");
        result.put("language", detectProgrammingLanguage(input));
        result.put("lines_of_code", input.split("\n").length);
        result.put("suggestions", Arrays.asList(
            "Consider adding input validation",
            "Add error handling for edge cases",
            "Optimize for better performance"
        ));

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"Failed to generate code explanation\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    private String generateTextSummarizer(String input) {
        String summary = "This is a generated summary of the provided text. It captures the main points and key information while maintaining the essential meaning of the original content.";
        
        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("original_length", input.length());
        result.put("summary_length", summary.length());
        result.put("compression_ratio", Math.round((double) summary.length() / input.length() * 100.0) / 100.0);
        result.put("key_topics", Arrays.asList("topic1", "topic2", "topic3"));

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"Failed to generate text summary\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    private String generateLanguageDetector(String input) {
        String[] languages = {"English", "Spanish", "French", "German", "Italian", "Portuguese", "Russian", "Chinese", "Japanese", "Korean"};
        String detectedLanguage = languages[random.nextInt(languages.length)];
        double confidence = 0.8 + random.nextDouble() * 0.2; // 0.8-1.0

        Map<String, Object> result = new HashMap<>();
        result.put("detected_language", detectedLanguage);
        result.put("confidence", Math.round(confidence * 100.0) / 100.0);
        result.put("language_code", getLanguageCode(detectedLanguage));
        result.put("text_length", input.length());

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"Failed to generate language detection\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    private String detectProgrammingLanguage(String code) {
        if (code.contains("public class") || code.contains("import java")) return "Java";
        if (code.contains("def ") || code.contains("import ")) return "Python";
        if (code.contains("function ") || code.contains("var ") || code.contains("const ")) return "JavaScript";
        if (code.contains("#include") || code.contains("int main")) return "C/C++";
        if (code.contains("package ") || code.contains("func ")) return "Go";
        return "Unknown";
    }

    private String getLanguageCode(String language) {
        Map<String, String> languageCodes = Map.of(
            "English", "en", "Spanish", "es", "French", "fr", "German", "de",
            "Italian", "it", "Portuguese", "pt", "Russian", "ru", "Chinese", "zh",
            "Japanese", "ja", "Korean", "ko"
        );
        return languageCodes.getOrDefault(language, "unknown");
    }
} 
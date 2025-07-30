package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Base64;

@Service
public class AIService {

    @Autowired
    private AIFunctionRepository aiFunctionRepository;

    @Autowired
    private AIExecutionRepository aiExecutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${google.gemini.api.key}")
    private String geminiApiKey;

    @Value("${google.gemini.model.name}")
    private String geminiModelName;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder()
        .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
        .build();

    // Predefined AI models with Gemini capabilities
    private static final Map<String, String> AI_MODELS = Map.of(
        "sentiment-analysis", "Sentiment Analysis",
        "image-labeling", "Image Labeling", 
        "code-explainer", "Code Explainer",
        "text-summarizer", "Text Summarizer",
        "language-detector", "Language Detector",
        "text-generator", "Text Generator",
        "code-generator", "Code Generator",
        "translation", "Translation"
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
        function.setMaxExecutionsPerDay(MAX_EXECUTIONS_PER_DAY);
        function.setCurrentExecutionsToday(0);
        function.setLastResetDate(LocalDateTime.now());

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

        // Process with Gemini AI
        long startTime = System.currentTimeMillis();
        try {
            String output = processWithGemini(function.getModel(), input, function.getType());
            execution.setOutput(output);
            execution.setStatus("success");
        } catch (Exception e) {
            execution.setOutput("{\"error\": \"AI processing failed\", \"message\": \"" + e.getMessage() + "\"}");
            execution.setStatus("error");
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        execution.setExecutionTimeMs(executionTime);
        execution.setOutputSizeBytes((long) execution.getOutput().getBytes().length);

        return aiExecutionRepository.save(execution);
    }

    private String processWithGemini(String model, String input, String type) throws Exception {
        String prompt = generatePrompt(model, input, type);
        
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> contents = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);
        parts.add(textPart);
        
        // Add image part if it's an image type and input is base64 image
        if (type.equals("image") && isBase64Image(input)) {
            Map<String, Object> imagePart = new HashMap<>();
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mime_type", "image/jpeg");
            inlineData.put("data", input);
            imagePart.put("inline_data", inlineData);
            parts.add(imagePart);
        }
        
        contents.put("parts", parts);
        requestBody.put("contents", Arrays.asList(contents));
        
        // Add safety settings
        Map<String, Object> safetySettings = new HashMap<>();
        safetySettings.put("category", "HARM_CATEGORY_HARASSMENT");
        safetySettings.put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
        requestBody.put("safety_settings", Arrays.asList(safetySettings));
        
        // Add generation config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("top_k", 40);
        generationConfig.put("top_p", 0.95);
        generationConfig.put("max_output_tokens", 2048);
        requestBody.put("generation_config", generationConfig);

        try {
            String response = webClient.post()
                .uri("/" + geminiModelName + ":generateContent?key=" + geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return parseGeminiResponse(response);
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Gemini API error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage());
        }
    }

    private String parseGeminiResponse(String response) throws Exception {
        Map<String, Object> responseMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        
        Object candidatesObj = responseMap.get("candidates");
        if (candidatesObj instanceof List<?>) {
            List<?> candidates = (List<?>) candidatesObj;
            if (!candidates.isEmpty() && candidates.get(0) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> candidate = (Map<String, Object>) candidates.get(0);
                
                Object contentObj = candidate.get("content");
                if (contentObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> content = (Map<String, Object>) contentObj;
                    
                    Object partsObj = content.get("parts");
                    if (partsObj instanceof List<?>) {
                        List<?> partsList = (List<?>) partsObj;
                        if (!partsList.isEmpty() && partsList.get(0) instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> part = (Map<String, Object>) partsList.get(0);
                            Object textObj = part.get("text");
                            if (textObj instanceof String) {
                                return (String) textObj;
                            }
                        }
                    }
                }
            }
        }
        
        // If no valid response, return error
        return "{\"error\": \"No valid response from Gemini API\"}";
    }

    private String generatePrompt(String model, String input, String type) {
        switch (model) {
            case "sentiment-analysis":
                return String.format(
                    "Analyze the sentiment of the following text and return a JSON response with sentiment (positive/negative/neutral), confidence (0.0-1.0), and explanation:\n\nText: %s\n\nRespond only with valid JSON.",
                    input
                );
                
            case "image-labeling":
                return "Analyze this image and identify all objects, people, animals, and scenes visible. Return a JSON response with an array of detected objects, each containing 'label', 'confidence' (0.0-1.0), and 'description'. Respond only with valid JSON.";
                
            case "code-explainer":
                return String.format(
                    "Explain the following code in detail. Return a JSON response with explanation, complexity, language, lines_of_code, and suggestions for improvement:\n\nCode:\n%s\n\nRespond only with valid JSON.",
                    input
                );
                
            case "text-summarizer":
                return String.format(
                    "Summarize the following text in a concise way. Return a JSON response with summary, original_length, summary_length, compression_ratio, and key_topics:\n\nText: %s\n\nRespond only with valid JSON.",
                    input
                );
                
            case "language-detector":
                return String.format(
                    "Detect the language of the following text. Return a JSON response with detected_language, confidence (0.0-1.0), language_code, and text_length:\n\nText: %s\n\nRespond only with valid JSON.",
                    input
                );
                
            case "text-generator":
                return String.format(
                    "Generate creative text based on the following prompt. Return a JSON response with generated_text, creativity_score (0.0-1.0), and word_count:\n\nPrompt: %s\n\nRespond only with valid JSON.",
                    input
                );
                
            case "code-generator":
                return String.format(
                    "Generate code based on the following description. Return a JSON response with generated_code, language, complexity, and explanation:\n\nDescription: %s\n\nRespond only with valid JSON.",
                    input
                );
                
            case "translation":
                return String.format(
                    "Translate the following text to English. Return a JSON response with translated_text, source_language, target_language, and confidence (0.0-1.0):\n\nText: %s\n\nRespond only with valid JSON.",
                    input
                );
                
            default:
                return String.format(
                    "Process the following input and provide a helpful response. Return a JSON response with result and any relevant metadata:\n\nInput: %s\n\nRespond only with valid JSON.",
                    input
                );
        }
    }

    private boolean isBase64Image(String input) {
        try {
            byte[] decoded = Base64.getDecoder().decode(input);
            // Check if it's a valid image by looking at magic bytes
            if (decoded.length >= 2) {
                return (decoded[0] == (byte) 0xFF && decoded[1] == (byte) 0xD8) || // JPEG
                       (decoded[0] == (byte) 0x89 && decoded[1] == (byte) 0x50) || // PNG
                       (decoded[0] == (byte) 0x47 && decoded[1] == (byte) 0x49);  // GIF
            }
        } catch (Exception e) {
            // Not base64 or not an image
        }
        return false;
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
        stats.put("aiModel", geminiModelName);
        stats.put("aiProvider", "Google Gemini");

        return stats;
    }

    public Map<String, String> getAvailableModels() {
        return new HashMap<>(AI_MODELS);
    }

    public List<String> getAvailableTypes() {
        return new ArrayList<>(FUNCTION_TYPES);
    }

    public Map<String, Object> getAIConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("model", geminiModelName);
        config.put("provider", "Google Gemini");
        config.put("status", "active");
        config.put("supportedTypes", FUNCTION_TYPES);
        config.put("supportedModels", AI_MODELS);
        return config;
    }
} 
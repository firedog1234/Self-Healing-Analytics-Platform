package com.selfhealing.analytics.aiops.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfhealing.analytics.common.model.DataQualityCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAI API integration for AI-powered incident analysis.
 * Uses GPT-4 or GPT-3.5-turbo for root cause analysis and remediation recommendations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIService implements AIService {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.openai.api-key:}")
    private String apiKey;
    
    @Value("${ai.openai.model:gpt-4o-mini}")
    private String model;
    
    @Value("${ai.openai.enabled:false}")
    private boolean enabled;
    
    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;
    
    private WebClient webClient;
    
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        }
        return webClient;
    }
    
    @Override
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty();
    }
    
    @Override
    public String generateRootCauseExplanation(List<DataQualityCheck> checks, String tableName, String classification) {
        if (!isAvailable()) {
            return null;
        }
        
        try {
            String prompt = buildRootCausePrompt(checks, tableName, classification);
            String response = callOpenAI(prompt, "You are an expert data engineer analyzing data quality incidents. Provide a concise, technical root cause explanation.");
            
            if (response != null && !response.trim().isEmpty()) {
                log.info("Generated AI root cause explanation for table: {}", tableName);
                return response.trim();
            }
        } catch (Exception e) {
            log.error("Error calling OpenAI for root cause analysis", e);
        }
        
        return null;
    }
    
    @Override
    public List<String> generateRemediations(List<DataQualityCheck> checks, String classification, String rootCauseExplanation) {
        if (!isAvailable()) {
            return null;
        }
        
        try {
            String prompt = buildRemediationPrompt(checks, classification, rootCauseExplanation);
            String response = callOpenAI(prompt, "You are an expert SRE providing actionable remediation steps for data quality incidents. Return a numbered list of specific, actionable steps.");
            
            if (response != null && !response.trim().isEmpty()) {
                List<String> remediations = parseRemediationList(response);
                log.info("Generated {} AI remediation recommendations", remediations.size());
                return remediations;
            }
        } catch (Exception e) {
            log.error("Error calling OpenAI for remediation recommendations", e);
        }
        
        return null;
    }
    
    @Override
    public String classifyIncident(List<DataQualityCheck> checks) {
        if (!isAvailable()) {
            return null;
        }
        
        try {
            String prompt = buildClassificationPrompt(checks);
            String response = callOpenAI(prompt, "You are an expert at classifying data quality incidents. Return only the classification category name (e.g., DATA_INGESTION_FAILURE, SCHEMA_COMPATIBILITY_ISSUE, etc.).");
            
            if (response != null && !response.trim().isEmpty()) {
                // Clean up response - remove quotes, whitespace, etc.
                String classification = response.trim()
                        .replaceAll("^[\"']|[\"']$", "")
                        .replaceAll("^Category:\\s*", "")
                        .trim()
                        .toUpperCase();
                log.info("Generated AI classification: {}", classification);
                return classification;
            }
        } catch (Exception e) {
            log.error("Error calling OpenAI for incident classification", e);
        }
        
        return null;
    }
    
    private String callOpenAI(String prompt, String systemMessage) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", systemMessage),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 500);
            
            String requestJson = objectMapper.writeValueAsString(requestBody);
            
            String responseJson = getWebClient()
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(throwable -> {
                                // Retry on network errors, not on API errors
                                return throwable instanceof java.net.ConnectException ||
                                       throwable.getMessage().contains("timeout");
                            }))
                    .block(Duration.ofSeconds(30));
            
            if (responseJson != null) {
                JsonNode jsonNode = objectMapper.readTree(responseJson);
                JsonNode choices = jsonNode.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null) {
                        JsonNode content = message.get("content");
                        if (content != null) {
                            return content.asText();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing OpenAI response", e);
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
        
        return null;
    }
    
    private String buildRootCausePrompt(List<DataQualityCheck> checks, String tableName, String classification) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze the following data quality incident and provide a root cause explanation.\n\n");
        sb.append("Table: ").append(tableName).append("\n");
        sb.append("Classification: ").append(classification).append("\n");
        sb.append("Failed Checks:\n");
        
        for (int i = 0; i < checks.size(); i++) {
            DataQualityCheck check = checks.get(i);
            sb.append(String.format("  %d. Type: %s, Status: %s", i + 1, check.getCheckType(), check.getStatus()));
            if (check.getActualValue() != null) {
                sb.append(String.format(", Actual Value: %.2f", check.getActualValue()));
            }
            if (check.getThreshold() != null) {
                sb.append(String.format(", Threshold: %.2f", check.getThreshold()));
            }
            if (check.getDetails() != null && !check.getDetails().isEmpty()) {
                sb.append(", Details: ").append(check.getDetails());
            }
            sb.append("\n");
        }
        
        sb.append("\nProvide a concise technical explanation of the root cause.");
        return sb.toString();
    }
    
    private String buildRemediationPrompt(List<DataQualityCheck> checks, String classification, String rootCauseExplanation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Based on the following incident, provide specific, actionable remediation steps.\n\n");
        sb.append("Classification: ").append(classification).append("\n");
        sb.append("Root Cause: ").append(rootCauseExplanation).append("\n");
        sb.append("Affected Checks: ").append(checks.size()).append("\n");
        
        Set<String> affectedTables = checks.stream()
                .map(DataQualityCheck::getTableName)
                .collect(Collectors.toSet());
        sb.append("Affected Tables: ").append(String.join(", ", affectedTables)).append("\n");
        
        sb.append("\nProvide a numbered list of specific remediation steps that can be taken to resolve this incident.");
        return sb.toString();
    }
    
    private String buildClassificationPrompt(List<DataQualityCheck> checks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Classify the following data quality incident into one of these categories:\n");
        sb.append("- DATA_INGESTION_FAILURE\n");
        sb.append("- DATA_QUALITY_DEGRADATION\n");
        sb.append("- SCHEMA_COMPATIBILITY_ISSUE\n");
        sb.append("- BATCH_JOB_FAILURE\n");
        sb.append("- UNKNOWN_DATA_ISSUE\n\n");
        
        sb.append("Failed Checks:\n");
        for (DataQualityCheck check : checks) {
            sb.append(String.format("  - Type: %s, Table: %s", check.getCheckType(), check.getTableName()));
            if (check.getDetails() != null) {
                sb.append(", Details: ").append(check.getDetails());
            }
            sb.append("\n");
        }
        
        sb.append("\nReturn only the classification category name.");
        return sb.toString();
    }
    
    private List<String> parseRemediationList(String response) {
        List<String> remediations = new ArrayList<>();
        String[] lines = response.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Remove numbering (1., 2., - , *, etc.)
            line = line.replaceAll("^\\d+[.)]\\s*", "")
                      .replaceAll("^[-*]\\s*", "")
                      .trim();
            
            if (!line.isEmpty() && line.length() > 10) { // Filter out very short items
                remediations.add(line);
            }
        }
        
        return remediations.isEmpty() ? Arrays.asList(response) : remediations;
    }
}

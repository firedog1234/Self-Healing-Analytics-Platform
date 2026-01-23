package com.selfhealing.analytics.aiops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provider that selects the appropriate AI service based on configuration.
 * Priority: OpenAI > Anthropic > Rule-based fallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AIServiceProvider {
    
    private final List<AIService> aiServices;
    
    @Value("${ai.provider:auto}")
    private String provider;
    
    /**
     * Get the best available AI service.
     * 
     * @return The configured AI service, or fallback to rule-based
     */
    public AIService getAIService() {
        // If explicitly configured, use that provider
        if (!"auto".equalsIgnoreCase(provider)) {
            for (AIService service : aiServices) {
                if (service.getClass().getSimpleName().toLowerCase().contains(provider.toLowerCase())) {
                    if (service.isAvailable()) {
                        log.info("Using explicitly configured AI provider: {}", service.getClass().getSimpleName());
                        return service;
                    } else {
                        log.warn("Configured provider {} is not available, falling back", provider);
                    }
                }
            }
        }
        
        // Auto-select: try OpenAI first, then Anthropic, then fallback
        for (AIService service : aiServices) {
            if (service.isAvailable() && !(service instanceof RuleBasedFallbackService)) {
                log.info("Auto-selected AI provider: {}", service.getClass().getSimpleName());
                return service;
            }
        }
        
        // Fallback to rule-based
        log.info("No AI providers available, using rule-based fallback");
        return aiServices.stream()
                .filter(s -> s instanceof RuleBasedFallbackService)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No AI service available"));
    }
}

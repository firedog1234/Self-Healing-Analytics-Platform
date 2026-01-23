package com.selfhealing.analytics.aiops.service;

import com.selfhealing.analytics.common.model.DataQualityCheck;
import java.util.List;

/**
 * Interface for AI-powered analysis services.
 * Supports multiple providers (OpenAI, Anthropic, local LLMs) with fallback to rule-based logic.
 */
public interface AIService {
    
    /**
     * Generate a root cause explanation using AI analysis of data quality checks.
     * 
     * @param checks List of failed data quality checks
     * @param tableName The affected table name
     * @param classification The incident classification
     * @return AI-generated root cause explanation
     */
    String generateRootCauseExplanation(List<DataQualityCheck> checks, String tableName, String classification);
    
    /**
     * Generate remediation recommendations using AI analysis.
     * 
     * @param checks List of failed data quality checks
     * @param classification The incident classification
     * @param rootCauseExplanation The root cause explanation
     * @return List of AI-generated remediation recommendations
     */
    List<String> generateRemediations(List<DataQualityCheck> checks, String classification, String rootCauseExplanation);
    
    /**
     * Classify an incident using AI analysis.
     * 
     * @param checks List of failed data quality checks
     * @return AI-generated incident classification
     */
    String classifyIncident(List<DataQualityCheck> checks);
    
    /**
     * Check if AI service is available and configured.
     * 
     * @return true if AI is available, false to use fallback
     */
    boolean isAvailable();
}

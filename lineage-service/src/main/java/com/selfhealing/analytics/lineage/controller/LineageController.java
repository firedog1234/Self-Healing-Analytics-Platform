package com.selfhealing.analytics.lineage.controller;

import com.selfhealing.analytics.lineage.service.LineageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lineage")
@RequiredArgsConstructor
public class LineageController {
    
    private final LineageService lineageService;
    
    @GetMapping("/affected/{tableName}")
    public ResponseEntity<List<Map<String, Object>>> getAffectedTables(@PathVariable String tableName) {
        return ResponseEntity.ok(lineageService.getAffectedTables(tableName));
    }
    
    @GetMapping("/dependencies/{jobName}")
    public ResponseEntity<List<Map<String, Object>>> getDownstreamDependencies(@PathVariable String jobName) {
        return ResponseEntity.ok(lineageService.getDownstreamDependencies(jobName));
    }
    
    @GetMapping("/impact/{tableName}")
    public ResponseEntity<List<Map<String, Object>>> getImpactAnalysis(@PathVariable String tableName) {
        return ResponseEntity.ok(lineageService.getImpactAnalysis(tableName));
    }
}

package com.selfhealing.analytics.incidentstore.controller;

import com.selfhealing.analytics.incidentstore.service.IncidentStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {
    
    private final IncidentStoreService incidentStoreService;
    
    @GetMapping("/similar/{classification}")
    public ResponseEntity<List<Map<String, Object>>> getSimilarIncidents(
            @PathVariable String classification) {
        return ResponseEntity.ok(incidentStoreService.getSimilarIncidents(classification));
    }
    
    @PostMapping("/{incidentId}/resolve")
    public ResponseEntity<Void> resolveIncident(
            @PathVariable String incidentId,
            @RequestBody Map<String, String> request) {
        String resolution = request.getOrDefault("resolution", "");
        incidentStoreService.resolveIncident(incidentId, resolution);
        return ResponseEntity.ok().build();
    }
}

package com.unbumpkin.codechat.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.unbumpkin.codechat.service.sse.SseService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/sse")
public class SseController {
    
    @Autowired
    private SseService sseService;
    
    
    /**
     * Generate a unique client ID for the SSE connection.
     * This ID can be used to identify the client in the system.
     * @return A ResponseEntity containing the client ID.
     */
    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> connect() {
        String clientId = sseService.getNewClientId();
        return ResponseEntity.ok(Map.of("clientId", clientId));
    }
    
    /**
     * Stream debug messages to the client.
     * This endpoint sets the necessary headers for SSE and creates an SseEmitter.
     * The client can use the returned emitter to receive messages.
     * @param clientId
     * @param response
     * @return
     */
    @GetMapping("/debug/{clientId}")
    public SseEmitter streamDebugMessages(@PathVariable String clientId, HttpServletResponse response) {
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Content-Type", "text/event-stream");
        
        SseEmitter emitter = sseService.createEmitter(clientId);
        sseService.sendMessage(clientId, "Starting server communication...");
        
        return emitter;
    }
    
    @GetMapping("/debug/stop/{clientId}")
    public ResponseEntity<String> stopDebugMessages(@PathVariable String clientId) {
        sseService.removeEmitter(clientId);
        return ResponseEntity.ok("Debug message stream stopped.");
    }
}
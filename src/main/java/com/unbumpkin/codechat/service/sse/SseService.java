package com.unbumpkin.codechat.service.sse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {
    private static final long TIMEOUT = 900_000L; // 15 minutes
    
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    public SseEmitter createEmitter(String clientId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        
        emitter.onCompletion(() -> removeEmitter(clientId));
        emitter.onTimeout(() -> removeEmitter(clientId));
        emitter.onError(e -> removeEmitter(clientId));
        
        emitters.put(clientId, emitter);
        return emitter;
    }
    
    public void removeEmitter(String clientId) {
        emitters.remove(clientId);
    }
    
    public void sendMessageToAll(String message) {
        emitters.forEach((clientId, emitter) -> sendMessage(clientId, message));
    }
    
    public void sendMessage(String clientId, String message) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) {
            System.out.println("No emitter found for client: " + clientId);
            return;
        }
        
        executor.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("debug")
                    .data(message));
            } catch (IOException | IllegalStateException e) {
                System.out.println("Error sending message to client " + clientId + ": " + e.getMessage());
                emitters.remove(clientId);
            }
        });
    }

    public String getNewClientId() {
        return UUID.randomUUID().toString();
    }

    @PreDestroy
    public void shutdown() {
        // First notify clients
        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("debug")
                    .data("Server shutting down..."));
                emitter.complete();
            } catch (Exception ignored) {}
        });
        
        // Clear emitters
        emitters.clear();
        
        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
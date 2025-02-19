package com.unbumpkin.codechat.controller;

import com.unbumpkin.codechat.domain.Message;
import com.unbumpkin.codechat.repository.MessageRepository;
import com.unbumpkin.codechat.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private int getUserIdFromAuthHeader(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping
    public ResponseEntity<String> addMessage(
        @RequestBody Message message, 
        @RequestBody int discussionId,
        @RequestHeader("Authorization") String authHeader
    ) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        messageRepository.addMessage(discussionId, message, authorId);
        return ResponseEntity.ok("Message added successfully");
    }

    @GetMapping("/{msgId}")
    public ResponseEntity<Message> getMessageById(
        @PathVariable int msgId, 
        @RequestHeader("Authorization") String authHeader
    ) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        Message message = messageRepository.getMessageById(msgId, authorId);
        return ResponseEntity.ok(message);
    }

    @GetMapping
    public ResponseEntity<List<Message>> getAllMessagesByDiscussionId(
        @RequestParam int discussionId, 
        @RequestHeader("Authorization") String authHeader
    ) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        List<Message> messages = messageRepository.getAllMessagesByDiscussionId(discussionId, authorId);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{msgId}")
    public ResponseEntity<String> updateMessage(
        @PathVariable int msgId, 
        @RequestBody Message message, 
        @RequestHeader("Authorization") String authHeader
    ) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        messageRepository.updateMessage(message, authorId);
        return ResponseEntity.ok("Message updated successfully");
    }

    @DeleteMapping("/{msgId}")
    public ResponseEntity<String> deleteMessage(
        @PathVariable int msgId, 
        @RequestHeader("Authorization") String authHeader
    ) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        messageRepository.deleteMessage(msgId, authorId);
        return ResponseEntity.ok("Message deleted successfully");
    }
}
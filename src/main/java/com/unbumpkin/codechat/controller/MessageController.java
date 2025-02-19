package com.unbumpkin.codechat.controller;

import com.unbumpkin.codechat.domain.Message;
import com.unbumpkin.codechat.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @PostMapping
    public ResponseEntity<String> addMessage(
        @RequestBody Message message, 
        @RequestBody int discussionId
    ) {
        messageRepository.addMessage(discussionId, message);
        return ResponseEntity.ok("Message added successfully");
    }

    @GetMapping("/{msgId}")
    public ResponseEntity<Message> getMessageById(@PathVariable int msgId) {
        Message message = messageRepository.getMessageById(msgId);
        return ResponseEntity.ok(message);
    }

    @GetMapping
    public ResponseEntity<List<Message>> getAllMessagesByDiscussionId(@RequestParam int discussionId) {
        List<Message> messages = messageRepository.getAllMessagesByDiscussionId(discussionId);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{msgId}")
    public ResponseEntity<String> updateMessage(
        @PathVariable int msgId, 
        @RequestBody Message message
    ) {
        messageRepository.updateMessage(message);
        return ResponseEntity.ok("Message updated successfully");
    }

    @DeleteMapping("/{msgId}")
    public ResponseEntity<String> deleteMessage(@PathVariable int msgId) {
        messageRepository.deleteMessage(msgId);
        return ResponseEntity.ok("Message deleted successfully");
    }
}
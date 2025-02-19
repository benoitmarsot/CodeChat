package com.unbumpkin.codechat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.unbumpkin.codechat.domain.Discussion;
import com.unbumpkin.codechat.repository.DiscussionRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discussions")
public class DiscussionController {

    @Autowired
    private DiscussionRepository discussionRepository;

    @PostMapping
    public ResponseEntity<Void> createDiscussion(@RequestBody Discussion discussion) {
        discussionRepository.addDiscussion(discussion);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{did}")
    public ResponseEntity<Discussion> getDiscussion(@PathVariable int did) {
        return ResponseEntity.ok(discussionRepository.getDiscussionById(did));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Discussion>> getDiscussionsByProject(@PathVariable int projectId) {
        return ResponseEntity.ok(discussionRepository.getAllDiscussionsByProjectId(projectId));
    }

    @PutMapping("/{did}")
    public ResponseEntity<Void> updateDiscussion(@PathVariable int did, @RequestBody Discussion discussion) {
        discussionRepository.updateDiscussion(discussion);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{did}")
    public ResponseEntity<Void> deleteDiscussion(@PathVariable int did) {
        discussionRepository.deleteDiscussion(did);
        return ResponseEntity.ok().build();
    }
}

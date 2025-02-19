package com.unbumpkin.codechat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.unbumpkin.codechat.domain.Project;
import com.unbumpkin.codechat.repository.ProjectRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    @PostMapping
    public ResponseEntity<Void> createProject(@RequestBody Project project) {
        projectRepository.addProject(project);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<Project> getProject(@PathVariable int projectId) {
        return ResponseEntity.ok(projectRepository.getProjectById(projectId));
    }

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(projectRepository.getAllProjects());
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<Void> updateProject(@PathVariable int projectId, @RequestBody Project project) {
        projectRepository.updateProject(project);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable int projectId) {
        projectRepository.deleteProject(projectId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}/users")
    public ResponseEntity<List<Integer>> getUsersWithAccess(@PathVariable int projectId) {
        return ResponseEntity.ok(projectRepository.getUsersWithAccess(projectId));
    }

    @PostMapping("/{projectId}/users/{userId}")
    public ResponseEntity<Void> grantAccess(@PathVariable int projectId, @PathVariable int userId) {
        projectRepository.grantUserAccessToProject(projectId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{projectId}/users/{userId}")
    public ResponseEntity<Void> revokeAccess(@PathVariable int projectId, @PathVariable int userId) {
        projectRepository.revokeUserAccessFromProject(projectId, userId);
        return ResponseEntity.ok().build();
    }
}
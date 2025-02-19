package com.unbumpkin.codechat.controller;

import com.unbumpkin.codechat.domain.Project;
import com.unbumpkin.codechat.repository.ProjectRepository;
import com.unbumpkin.codechat.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private int getUserIdFromAuthHeader(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping
    public ResponseEntity<String> addProject(
        @RequestBody Project project, 
        @RequestHeader("Authorization") String authHeader
    ) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        projectRepository.addProject(project, authorId);
        return ResponseEntity.ok("Project added successfully");
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<Project> getProjectById(@PathVariable int projectId, @RequestHeader("Authorization") String authHeader) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        Project project = projectRepository.getProjectById(projectId, authorId);
        return ResponseEntity.ok(project);
    }

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects(@RequestHeader("Authorization") String authHeader) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        List<Project> projects = projectRepository.getAllProjects(authorId);
        return ResponseEntity.ok(projects);
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<String> updateProject(@PathVariable int projectId, @RequestBody Project project, @RequestHeader("Authorization") String authHeader) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        projectRepository.updateProject(project, authorId);
        return ResponseEntity.ok("Project updated successfully");
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<String> deleteProject(@PathVariable int projectId, @RequestHeader("Authorization") String authHeader) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        projectRepository.deleteProject(projectId, authorId);
        return ResponseEntity.ok("Project deleted successfully");
    }

    @GetMapping("/{projectId}/users")
    public ResponseEntity<List<Integer>> getUsersWithAccess(@PathVariable int projectId, @RequestHeader("Authorization") String authHeader) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        List<Integer> users = projectRepository.getUsersWithAccess(projectId, authorId);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/{projectId}/users/{userId}")
    public ResponseEntity<String> grantUserAccessToProject(@PathVariable int projectId, @PathVariable int userId, @RequestHeader("Authorization") String authHeader) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        projectRepository.grantUserAccessToProject(projectId, userId, authorId);
        return ResponseEntity.ok("User access granted successfully");
    }

    @DeleteMapping("/{projectId}/users/{userId}")
    public ResponseEntity<String> revokeUserAccessFromProject(@PathVariable int projectId, @PathVariable int userId, @RequestHeader("Authorization") String authHeader) {
        int authorId = getUserIdFromAuthHeader(authHeader);
        projectRepository.revokeUserAccessFromProject(projectId, userId, authorId);
        return ResponseEntity.ok("User access revoked successfully");
    }
}
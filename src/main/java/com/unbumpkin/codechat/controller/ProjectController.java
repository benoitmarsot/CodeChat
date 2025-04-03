package com.unbumpkin.codechat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.unbumpkin.codechat.dto.response.ProjectWithResource;
import com.unbumpkin.codechat.model.Project;
import com.unbumpkin.codechat.model.ProjectResource;
import com.unbumpkin.codechat.model.openai.Assistant;
import com.unbumpkin.codechat.model.openai.OaiFile;
import com.unbumpkin.codechat.model.openai.VectorStore;
import com.unbumpkin.codechat.repository.ProjectRepository;
import com.unbumpkin.codechat.repository.ProjectResourceRepository;
import com.unbumpkin.codechat.repository.openai.AssistantRepository;
import com.unbumpkin.codechat.repository.openai.VectorStoreRepository;
import com.unbumpkin.codechat.service.openai.AssistantService;
import com.unbumpkin.codechat.service.openai.OaiFileService;
import com.unbumpkin.codechat.service.openai.VectorStoreService;

import io.github.classgraph.Resource;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;





@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectResourceRepository projectResourceRepository;
    @Autowired
    private AssistantRepository assistantRepository;
    @Autowired
    private VectorStoreRepository vectorStoreRepository;
    @Autowired
    private OaiFileService oaiFileService;

    @Autowired
    private AssistantService assistantService;

    @PostMapping
    public ResponseEntity<Void> createProject(@RequestBody Project project) {
        projectRepository.addProject(project.name(),project.description());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectWithResource> getProject(@PathVariable int projectId) {
        return ResponseEntity.ok(projectRepository.getProjectWithResById(projectId));
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

    @GetMapping("/{projectId}/markdeleted")
    public ResponseEntity<Void> markdeleted(
        @PathVariable int projectId
    ) {
        projectRepository.markForDeletion(projectId);
        return ResponseEntity.ok().build();
    }
    
    
    // todo: move that delete all to codechat controller (maybe)
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable int projectId) {
        // Project project=projectRepository.getProjectById(projectId);
        // Assistant assistant=assistantRepository.getAssistantById(project.assistantId());
        // List<String> vsFiles=vectorStoreRepository.findVectorStoreFiles(assistant.fullvsid());
        // vsFiles.forEach(vsFile->{
        //     try {
        //         oaiFileService.deleteFile(vsFile);
        //         System.out.println("Deleted file: "+vsFile);
        //     } catch (Exception e) {
        //         System.out.println("Error deleting file: "+vsFile);
        //     }
        // });
        // VectorStore vs=vectorStoreRepository.getVectorStoreById(assistant.codevsid());
        // try {
        //     vectorStoreRepository.deleteVectorStore((assistant.codevsid());
        //     System.out.println("Deleted file: "+assistant.configvsid());
        // } catch (Exception e) {
        //     System.out.println("Error deleting file: "+assistant.configvsid());
        // }
        
        // Only creating the code vs for now
        // vectorStoreRepository.deleteVectorStore(assistant.markupvsid());
        // vectorStoreRepository.deleteVectorStore(assistant.configvsid());
        // vectorStoreRepository.deleteVectorStore(assistant.fullvsid());
        


        
        // //Todo: delete everything that have to do with the project, including:
        // // - the assistant 
        // // - the vector stores
        // // - and the oaifiles
        // assistant.codevsid()
        // assistantService.deleteAssistant(project.assistantId());
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
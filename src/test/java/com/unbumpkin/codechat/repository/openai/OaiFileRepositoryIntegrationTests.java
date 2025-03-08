package com.unbumpkin.codechat.repository.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.unbumpkin.codechat.config.TestSecurityConfig;
import com.unbumpkin.codechat.model.Project;
import com.unbumpkin.codechat.model.openai.OaiFile;
import com.unbumpkin.codechat.repository.ProjectRepository;

@SpringBootTest
@Transactional // Rollback after each test, so no need to clean up
@Import(TestSecurityConfig.class)
public class OaiFileRepositoryIntegrationTests {
    @Autowired
    private OaiFileRepository oaiFileRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private AssistantRepository assistantRepository;

    private static int TEST_USER_ID = 0;
    private static int PROJECT_ID = 0;
    private static int ASSISTANT_ID = 0;

    // @BeforeEach
    // public void setup() {
    //     PROJECT_ID = projectRepository.createProject(
    //         new Project(0, "Test Project", "Test Description", TEST_USER_ID, ASSISTANT_ID
    //     ));

    // }

    // @Test
    // public void testStoreAndRetrieveOaiFile() throws JsonProcessingException, DataAccessException {
    //     OaiFile file = new OaiFile(0, TEST_USER_ID, "file1", "Test File", "/root", "/path", 
    //         OaiFile.Purposes.assistants, 100);
    //     oaiFileRepository.storeOaiFile(file);

    //     OaiFile retrieved = oaiFileRepository.retrieveFile("file1");
    //     assertNotNull(retrieved);
    //     assertEquals("Test File", retrieved.fileName());
    //     assertTrue(retrieved.fId() > 0);
    // }

    // @Test
    // public void testStoreAndRetrieveMultipleOaiFiles() throws Exception {
    //     OaiFile file1 = new OaiFile(0, TEST_USER_ID, "file1", "Test File 1", "/root", "/path1", 
    //         OaiFile.Purposes.assistants, 100);
    //     OaiFile file2 = new OaiFile(0, TEST_USER_ID, "file2", "Test File 2", "/root", "/path2", 
    //         OaiFile.Purposes.assistants, 200);
    //     oaiFileRepository.storeOaiFiles(List.of( file1, file2));

    //     List<OaiFile> retrievedFiles = oaiFileRepository.retrieveFiles(List.of("file1", "file2"));
    //     assertEquals(2, retrievedFiles.size());
    // }

    // @Test
    // public void testDeleteOaiFile() throws JsonProcessingException, DataAccessException {
    //     OaiFile file = new OaiFile(0, TEST_USER_ID, "file1", "Test File", "/root", "/path", 
    //         OaiFile.Purposes.assistants, 100);
    //     oaiFileRepository.storeOaiFile(file);

    //     oaiFileRepository.deleteFile("file1");
    //     List<OaiFile> retrievedFiles = oaiFileRepository.retrieveFiles(List.of("file1"));
    //     assertTrue(retrievedFiles.isEmpty());
    // }

    // @Test
    // public void testListAllFiles() throws JsonProcessingException, DataAccessException {
    //     int size=oaiFileRepository.countFiles();
    //     OaiFile file1 = new OaiFile(0, TEST_USER_ID, "file1", "Test File 1", "/root", "/path1", 
    //         OaiFile.Purposes.assistants, 100);
    //     OaiFile file2 = new OaiFile(0, TEST_USER_ID, "file2", "Test File 2", "/root", "/path2", 
    //         OaiFile.Purposes.assistants, 200);
    //     oaiFileRepository.storeOaiFile(file1);
    //     oaiFileRepository.storeOaiFile(file2);

    //     List<OaiFile> allFiles = oaiFileRepository.listAllFiles();
    //     assertEquals(2+size, allFiles.size());
    // }

    // @Test
    // public void testRetrieveFilesByRootDir() throws JsonProcessingException, DataAccessException {
    //     OaiFile file1 = new OaiFile(0, TEST_USER_ID, "file1", "Test File 1", "/root1", "/path1", 
    //         OaiFile.Purposes.assistants, 100);
    //     OaiFile file2 = new OaiFile(0, TEST_USER_ID, "file2", "Test File 2", "/root1", "/path2", 
    //         OaiFile.Purposes.assistants, 200);
    //     OaiFile file3 = new OaiFile(0, TEST_USER_ID, "file3", "Test File 3", "/root2", "/path3", 
    //         OaiFile.Purposes.assistants, 300);
        
    //     oaiFileRepository.storeOaiFile(file1);
    //     oaiFileRepository.storeOaiFile(file2);
    //     oaiFileRepository.storeOaiFile(file3);

    //     List<OaiFile> root1Files = oaiFileRepository.retrieveFiles("/root1");
    //     assertEquals(2, root1Files.size());
    //     assertTrue(root1Files.stream().allMatch(f -> f.rootdir().equals("/root1")));
    // }

    // @Test
    // public void testDeleteMultipleFiles() throws JsonProcessingException, DataAccessException {
    //     int count = oaiFileRepository.countFiles();
    //     OaiFile file1 = new OaiFile(0, TEST_USER_ID, "file1", "Test File 1", "/root", "/path1", 
    //         OaiFile.Purposes.assistants, 100);
    //     OaiFile file2 = new OaiFile(0, TEST_USER_ID, "file2", "Test File 2", "/root", "/path2", 
    //         OaiFile.Purposes.assistants, 200);
    //     oaiFileRepository.storeOaiFile(file1);
    //     oaiFileRepository.storeOaiFile(file2);

    //     oaiFileRepository.deleteFiles(List.of("file1", "file2"));
    //     List<OaiFile> allFiles = oaiFileRepository.listAllFiles();
    //     assertTrue(count==allFiles.size());
    // }
}
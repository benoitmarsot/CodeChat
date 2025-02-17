package com.unbumpkin.codechat.repository.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.unbumpkin.codechat.domain.openai.OaiFile;
import com.unbumpkin.codechat.repository.UserRepository;
@SpringBootTest
@Transactional // Rollback after each test, so no need to clean up
public class OaiFileRepositoryIntegrationTests {
    @Autowired
    private OaiFileRepository oaiFileRepository;
    @Autowired
    private UserRepository userRepository;

    private static int TEST_USER_ID = 0;
    @BeforeEach
    public void setUp() {
        if(TEST_USER_ID == 0) {
            TEST_USER_ID = userRepository.findFirstUserId();
        }
    }

    @Test
    public void testStoreAndRetrieveOaiFile() throws JsonProcessingException, DataAccessException {
        OaiFile file = new OaiFile(0, TEST_USER_ID, "file1", "Test File", "/root", "/path", 
            OaiFile.Purposes.assistants, 100);
        oaiFileRepository.storeOaiFile(file,TEST_USER_ID);

        OaiFile retrieved = oaiFileRepository.retrieveFile("file1",TEST_USER_ID);
        assertNotNull(retrieved);
        assertEquals("Test File", retrieved.fileName());
        assertTrue(retrieved.fId() > 0);
    }

    @Test
    public void testStoreAndRetrieveMultipleOaiFiles() throws Exception {
        OaiFile file1 = new OaiFile(0, TEST_USER_ID, "file1", "Test File 1", "/root", "/path1", 
            OaiFile.Purposes.assistants, 100);
        OaiFile file2 = new OaiFile(0, TEST_USER_ID, "file2", "Test File 2", "/root", "/path2", 
            OaiFile.Purposes.assistants, 200);
        oaiFileRepository.storeOaiFiles(List.of( file1, file2),TEST_USER_ID);

        List<OaiFile> retrievedFiles = oaiFileRepository.retrieveFiles(List.of("file1", "file2"),TEST_USER_ID);
        assertEquals(2, retrievedFiles.size());
    }

    @Test
    public void testDeleteOaiFile() throws JsonProcessingException, DataAccessException {
        OaiFile file = new OaiFile(0, TEST_USER_ID, "file1", "Test File", "/root", "/path", 
            OaiFile.Purposes.assistants, 100);
        oaiFileRepository.storeOaiFile(file,TEST_USER_ID);

        oaiFileRepository.deleteFile("file1",TEST_USER_ID);
        List<OaiFile> retrievedFiles = oaiFileRepository.retrieveFiles(List.of("file1"),TEST_USER_ID);
        assertTrue(retrievedFiles.isEmpty());
    }

    @Test
    public void testListAllFiles() throws JsonProcessingException, DataAccessException {
        int size=oaiFileRepository.countFiles(TEST_USER_ID);
        OaiFile file1 = new OaiFile(0, TEST_USER_ID, "file1", "Test File 1", "/root", "/path1", 
            OaiFile.Purposes.assistants, 100);
        OaiFile file2 = new OaiFile(0, TEST_USER_ID, "file2", "Test File 2", "/root", "/path2", 
            OaiFile.Purposes.assistants, 200);
        oaiFileRepository.storeOaiFile(file1,TEST_USER_ID);
        oaiFileRepository.storeOaiFile(file2,TEST_USER_ID);

        List<OaiFile> allFiles = oaiFileRepository.listAllFiles(TEST_USER_ID);
        assertEquals(2+size, allFiles.size());
    }

    @Test
    public void testRetrieveFilesByRootDir() throws JsonProcessingException, DataAccessException {
        OaiFile file1 = new OaiFile(0, TEST_USER_ID, "file1", "Test File 1", "/root1", "/path1", 
            OaiFile.Purposes.assistants, 100);
        OaiFile file2 = new OaiFile(0, TEST_USER_ID, "file2", "Test File 2", "/root1", "/path2", 
            OaiFile.Purposes.assistants, 200);
        OaiFile file3 = new OaiFile(0, TEST_USER_ID, "file3", "Test File 3", "/root2", "/path3", 
            OaiFile.Purposes.assistants, 300);
        
        oaiFileRepository.storeOaiFile(file1,TEST_USER_ID);
        oaiFileRepository.storeOaiFile(file2,TEST_USER_ID);
        oaiFileRepository.storeOaiFile(file3,TEST_USER_ID);

        List<OaiFile> root1Files = oaiFileRepository.retrieveFiles("/root1",TEST_USER_ID);
        assertEquals(2, root1Files.size());
        assertTrue(root1Files.stream().allMatch(f -> f.rootdir().equals("/root1")));
    }

    @Test
    public void testDeleteMultipleFiles() throws JsonProcessingException, DataAccessException {
        int count = oaiFileRepository.countFiles(TEST_USER_ID);
        OaiFile file1 = new OaiFile(0, TEST_USER_ID, "file1", "Test File 1", "/root", "/path1", 
            OaiFile.Purposes.assistants, 100);
        OaiFile file2 = new OaiFile(0, TEST_USER_ID, "file2", "Test File 2", "/root", "/path2", 
            OaiFile.Purposes.assistants, 200);
        oaiFileRepository.storeOaiFile(file1,TEST_USER_ID);
        oaiFileRepository.storeOaiFile(file2,TEST_USER_ID);

        oaiFileRepository.deleteFiles(List.of("file1", "file2"),TEST_USER_ID);
        List<OaiFile> allFiles = oaiFileRepository.listAllFiles(TEST_USER_ID);
        assertTrue(count==allFiles.size());
    }
}
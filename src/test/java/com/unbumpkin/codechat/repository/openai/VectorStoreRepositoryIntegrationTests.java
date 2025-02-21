package com.unbumpkin.codechat.repository.openai;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.unbumpkin.codechat.config.TestSecurityConfig;
import com.unbumpkin.codechat.domain.openai.OaiFile;
import com.unbumpkin.codechat.domain.openai.VectorStore;
import com.unbumpkin.codechat.domain.openai.VectorStore.Static;
import com.unbumpkin.codechat.repository.openai.VectorStoreRepository.RepoVectorStoreResponse;
import com.unbumpkin.codechat.domain.openai.VectorStore.ChunkingStrategy;
import com.unbumpkin.codechat.domain.openai.VectorStore.ExpiresAfter;

@SpringBootTest
@Transactional
@Import(TestSecurityConfig.class)
public class VectorStoreRepositoryIntegrationTests {

    @Autowired
    private VectorStoreRepository vectorStoreRepository;
    @Autowired
    private OaiFileRepository fileRepository;

    private static final String VS_ID_1 = "vs1";
    private static final String VS_ID_2 = "vs2";

    private static int TEST_USER_ID = 0;
    // @BeforeEach
    // public void setUp() {
    //     if(TEST_USER_ID == 0) {
    //         TEST_USER_ID = userRepository.findFirstUserId();
    //     }
    // }

    // @Test
    // public void testStoreAndRetrieveVectorStore() throws JsonProcessingException {
    //     VectorStore vectorStore = createTestVectorStore(VS_ID_1);
    //     vectorStoreRepository.storeVectorStore(vectorStore);

    //     RepoVectorStoreResponse retrieved = vectorStoreRepository.getVectorStoreByOaiId(VS_ID_1);
    //     assertNotNull(retrieved);
    //     assertEquals("Test Vector Store", retrieved.name());
    //     assertEquals("Description", retrieved.description());
    //     assertNotNull(retrieved.created());
    //     assertEquals(30, retrieved.dayskeep());
    // }

    // @Test
    // public void testUpdateVectorStore() throws JsonProcessingException {
    //     VectorStore vectorStore = createTestVectorStore(VS_ID_1);
    //     vectorStoreRepository.storeVectorStore(vectorStore);

    //     vectorStore.setVsname("Updated Vector Store");
    //     vectorStore.setVsdesc("Updated Description");
    //     vectorStoreRepository.updateVectorStore(vectorStore);

    //     RepoVectorStoreResponse updated = vectorStoreRepository.getVectorStoreByOaiId(VS_ID_1);
    //     assertNotNull(updated);
    //     assertEquals("Updated Vector Store", updated.name());
    //     assertEquals("Updated Description", updated.description());
    // }

    // @Test
    // public void testGetAllVectorStores() throws JsonProcessingException {
    //     int count = vectorStoreRepository.getAllVectorStores().size();
    //     vectorStoreRepository.storeVectorStore(createTestVectorStore(VS_ID_1));
    //     vectorStoreRepository.storeVectorStore(createTestVectorStore(VS_ID_2));

    //     List<RepoVectorStoreResponse> vectorStores = vectorStoreRepository.getAllVectorStores();
    //     assertEquals(count+2, vectorStores.size());
    // }

    // @Test
    // public void testAddAndRemoveFile() throws JsonProcessingException {
    //     //First create the vector store
    //     VectorStore vectorStore = createTestVectorStore(VS_ID_1);
    //     vectorStoreRepository.storeVectorStore(vectorStore);
    //     RepoVectorStoreResponse vsRetrived=vectorStoreRepository.getVectorStoreByOaiId(VS_ID_1);
    //     assertNotNull(vsRetrived);

    //     // Create and store the file first
    //     String fileId = "file1";
    //     OaiFile file = new OaiFile(0, TEST_USER_ID, fileId, "test.txt", "/root", "/path/test.txt", 
    //         OaiFile.Purposes.assistants, 100);
    //     fileRepository.storeOaiFile(file);
    //     OaiFile fileRetrieved=fileRepository.retrieveFile( fileId);
    //     assertNotNull(fileRetrieved);

    //     // Now add the association and test
    //     vectorStoreRepository.addFile(VS_ID_1, fileId);
    //     assertTrue(vectorStoreRepository.vectorContainFile(VS_ID_1, fileId));

    //     // Remove and verify
    //     vectorStoreRepository.removeFile(VS_ID_1, fileId);
    //     assertFalse(vectorStoreRepository.vectorContainFile(VS_ID_1, fileId));
    // }

    // @Test
    // public void testAddMultipleFiles() throws JsonProcessingException {
    //     VectorStore vectorStore = createTestVectorStore(VS_ID_1);
    //     vectorStoreRepository.storeVectorStore(vectorStore);

    //     OaiFile file1 = new OaiFile(0, TEST_USER_ID, "file1", "Test File 1", "/root1", "/path1", 
    //         OaiFile.Purposes.assistants, 100);
    //     OaiFile file2 = new OaiFile(0, TEST_USER_ID, "file2", "Test File 2", "/root1", "/path2", 
    //         OaiFile.Purposes.assistants, 200);
    //     OaiFile file3 = new OaiFile(0, TEST_USER_ID, "file3", "Test File 3", "/root2", "/path3", 
    //         OaiFile.Purposes.assistants, 300);
        
    //     fileRepository.storeOaiFiles(List.of( file1, file2, file3));
    //     List<String> fileIds = List.of("file1", "file2", "file3");
    //     vectorStoreRepository.addFiles(VS_ID_1, (Collection<String>)fileIds);
        

    //     List<String> containedFiles = vectorStoreRepository.findVectorStoreFiles(VS_ID_1);
    //     assertEquals(fileIds.size(), containedFiles.size());
    //     assertTrue(containedFiles.containsAll(fileIds));
    // }

    // private VectorStore createTestVectorStore(String id) {
    //     VectorStore vectorStore = new VectorStore();
    //     vectorStore.setOaiVsId(id);
    //     vectorStore.setVsname("Test Vector Store");
    //     vectorStore.setVsdesc("Description");
    //     vectorStore.setCreated(Instant.now());
    //     vectorStore.setDayskeep(30);
    //     vectorStore.setExpiresAfter(new ExpiresAfter(30));
    //     vectorStore.setChunkingStrategy(new ChunkingStrategy(new Static(1000, 200)));
    //     vectorStore.setMetadata(Map.of("test", "value"));
    //     return vectorStore;
    // }
}
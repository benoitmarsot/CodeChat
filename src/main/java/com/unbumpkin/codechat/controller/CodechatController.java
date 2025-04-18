package com.unbumpkin.codechat.controller;

import static java.lang.String.format;
import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.unbumpkin.codechat.service.openai.AssistantBuilder;
import com.unbumpkin.codechat.service.openai.AssistantService;
import com.unbumpkin.codechat.service.openai.OaiFileService;
import com.unbumpkin.codechat.service.openai.ResponsesService;
import com.unbumpkin.codechat.service.openai.VectorStoreFile;
import com.unbumpkin.codechat.service.openai.AssistantBuilder.ReasoningEfforts;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;
import com.unbumpkin.codechat.service.openai.GithubRepoContentManager;
import com.unbumpkin.codechat.service.openai.CCProjectFileManager;
import static com.unbumpkin.codechat.service.openai.CCProjectFileManager.getFileType;
import static com.unbumpkin.codechat.util.ExtMimeType.getMimeType;
import com.unbumpkin.codechat.service.openai.CCProjectFileManager.Types;
import com.unbumpkin.codechat.service.openai.VectorStoreService;
import com.unbumpkin.codechat.service.openai.WebCrawlerContentManager;
import com.unbumpkin.codechat.service.openai.ZipContentManager;
import com.unbumpkin.codechat.util.ExtMimeType;
import com.unbumpkin.codechat.util.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.unbumpkin.codechat.dto.FileRenameDescriptor;
import com.unbumpkin.codechat.dto.GitHubChangeTracker;
import com.unbumpkin.codechat.dto.openai.Assistant;
import com.unbumpkin.codechat.dto.request.AddRepoRequest;
import com.unbumpkin.codechat.dto.request.AddWebRequest;
import com.unbumpkin.codechat.dto.request.AddZipRequest;
import com.unbumpkin.codechat.dto.request.CreateProjectRequest;
import com.unbumpkin.codechat.dto.request.CreateVSFileRequest;
import com.unbumpkin.codechat.dto.request.OaiImageDescResponsesRequest;
import com.unbumpkin.codechat.dto.request.OaiImageDescResponsesRequest.Details;
import com.unbumpkin.codechat.model.Project;
import com.unbumpkin.codechat.model.ProjectResource;
import com.unbumpkin.codechat.model.UserSecret;
import com.unbumpkin.codechat.model.ProjectResource.ResTypes;
import com.unbumpkin.codechat.model.UserSecret.Labels;
import com.unbumpkin.codechat.model.openai.OaiFile;
import com.unbumpkin.codechat.model.openai.VectorStore;
import com.unbumpkin.codechat.model.openai.OaiFile.Purposes;
import com.unbumpkin.codechat.repository.DiscussionRepository;
import com.unbumpkin.codechat.repository.MessageRepository;
import com.unbumpkin.codechat.repository.ProjectRepository;
import com.unbumpkin.codechat.repository.ProjectResourceRepository;
import com.unbumpkin.codechat.repository.openai.AssistantRepository;
import com.unbumpkin.codechat.repository.openai.OaiFileRepository;
import com.unbumpkin.codechat.repository.openai.OaiThreadRepository;
import com.unbumpkin.codechat.repository.openai.VectorStoreRepository;
import com.unbumpkin.codechat.repository.openai.VectorStoreRepository.RepoVectorStoreResponse;
import com.unbumpkin.codechat.repository.social.SocialChannelRepository;
import com.unbumpkin.codechat.repository.social.SocialUserRepository;
import com.unbumpkin.codechat.security.CustomAuthentication;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.unbumpkin.codechat.model.DebugMessage;

import jakarta.servlet.http.HttpServletResponse;



@RestController
@RequestMapping("/api/v1/codechat")
public class CodechatController {
    private static final int PRIORITY_MESSAGE_LIMIT = 1; // Define the constant
    private static final long TIME_OUT_SSL = 900_000L; // Set timeout to 15 minutes
    @Autowired
    private AssistantRepository assistantRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private OaiFileRepository oaiFileRepository;
    @Autowired
    private OaiFileService oaiFileService;
    @Autowired
    private VectorStoreRepository vsRepository;
    @Autowired
    private VectorStoreService vsService;
    @Autowired
    private AssistantService assistantService;
    @Autowired 
    private MessageRepository messageRepository;
    @Autowired
    private OaiThreadRepository threadRepository;
    @Autowired
    DiscussionRepository discussionRepository;
    @Autowired
    ProjectResourceRepository projectResourceRepository;
    @Autowired
    ResponsesService responsesService;
    @Autowired
    private SocialUserRepository socialUserRepository;
    @Autowired
    private SocialChannelRepository socialChannelRepository;
    private SseEmitter emitter;

    private int getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthentication) {
            return ((CustomAuthentication) authentication).getUserId();
        }
        throw new IllegalStateException("No authenticated user found");
    }

    @DeleteMapping("delete-all")
    public ResponseEntity<String> deleteAll(
    ) throws IOException {
        oaiFileService.cleanUpFiles();
        vsService.cleanUpVectorStores();
        assistantService.cleanUpAssistants();
        
        // Delete all records in the message table
        messageRepository.deleteAll();
        // Delete all records in the thread table
        threadRepository.deleteAll();
        // Delete all records in the assistant table
        assistantRepository.deleteAll();
        // Delete all records in the vectorstore table
        // - and the vectorstore_oaifile associations
        vsRepository.deleteAll();
        // Delete all records in the oaifile table
        oaiFileRepository.deleteAll();            
        // Delete all records in the sharedproject table
        projectRepository.deleteAll();
        // Delete all records in the discussion table
        //  - including vectorstore_discussion associations
        discussionRepository.deleteAll();
        // Delete all records in the project table
        projectRepository.deleteAll();
        // Delete all records in the socialuser table
        socialUserRepository.deleteAll();
        // Delete all records in the socialchannel table
        socialChannelRepository.deleteAll();
        return ResponseEntity.ok("All data deleted");

    }

    @PostMapping("describe-image")
    public ResponseEntity<String> describeImage(
        @RequestBody OaiImageDescResponsesRequest imageRequest
    ) throws IOException {
        return ResponseEntity.ok( 
            responsesService.describeImage(imageRequest,true)
        );
    }
    
    @Transactional
    @PostMapping("create-empty-project")
    public ResponseEntity<Project> createEmptyProject(
        @RequestBody CreateProjectRequest request
    ) throws Exception {
        int projectId=projectRepository.addProject(request.name(), request.description());
        if(projectId==0){
            throw new Exception("project could not be created.");
        }
        writeMessage("project created with id: "+projectId);

        Map<Types,VectorStore> vectorStoreMap = createEmptyVectorStores(projectId);
        writeMessage("Create assistant...");
        int assistantId=createAssistant(request.name(), projectId, vectorStoreMap);
        writeMessage("Assistant created with id: "+assistantId);
        Project project = new Project(projectId, request.name(), request.description(), this.getCurrentUserId(), assistantId);
        return ResponseEntity.ok(project);
    }
    @Transactional
    @PostMapping("add-project-web")
    public ResponseEntity<ProjectResource> addWebResource(
        @RequestBody AddWebRequest request
    ) throws Exception {
        int projectId = request.projectId();
        WebCrawlerContentManager webManager = null;
        
        try {
            // Create secret map to store credentials if provided
            Map<Labels,UserSecret> userSecrets = new HashMap<>();
            if(request.userName() != null && !request.userName().isEmpty()){
                userSecrets.put(Labels.username, new UserSecret(Labels.username, request.userName()));
                if(request.password() != null) {
                    userSecrets.put(Labels.password, new UserSecret(Labels.password, request.password()));
                }
            }
                
            // Create project resource
            ProjectResource resource = projectResourceRepository.createResource(
                projectId, request.seedUrl(), ResTypes.web, userSecrets
            );
            // Create the web crawler with custom settings if provided
            if (request.maxPages() > 0 || request.maxDepth() > 0 || request.requestsPerMinute() > 0) {
                int maxPages = request.maxPages() > 0 ? request.maxPages() : 100;
                int maxDepth = request.maxDepth() > 0 ? request.maxDepth() : 2;
                int reqPerMin = request.requestsPerMinute() > 0 ? request.requestsPerMinute() : 30;
                webManager = new WebCrawlerContentManager(
                    maxPages, maxDepth, 4, reqPerMin, true, true
                );
            } else {
                webManager = new WebCrawlerContentManager(); // Use defaults
            }
            
            // Start crawling
             writeMessage("Starting web crawl from URL: " + request.seedUrl());
            String crawlResult = request.allowedDomains() != null && !request.allowedDomains().isEmpty() ?
                webManager.crawlWebsite(request.seedUrl(), request.allowedDomains()) :
                webManager.crawlWebsite(request.seedUrl());
             writeMessage("Crawl result: " + crawlResult);
                        
            // Get vector stores for the project
            Map<Types, RepoVectorStoreResponse> vsMap = CCProjectFileManager.getVectorStoretMap(
                vsRepository.getVectorStoresByProjectId(projectId)
            );
            
            // Create vector store file services map for different types of vector stores
            Map<Types, VectorStoreFile> vsfServicesMap = getVsfServicesMap(vsMap);
            int tempDirLength = webManager.getTempDir().length();
            Set<File> files = webManager.getAllFiles();
             writeMessage("Found " + files.size() + " files to process.");
            int processedFiles = 0;
            // Process files
            for (File file : files) {

                try {
                    if (deleteIfExists(file.getPath().substring(tempDirLength+1), projectId, vsfServicesMap, tempDirLength)) {
                         writeMessage("The file " + file.getPath().substring(tempDirLength + 1) + 
                            " already exists it will be refreshed.");
                    }
                    String protocol=webManager.getSeedUrl().substring(0, webManager.getSeedUrl().indexOf("/"));
                    addFile(webManager.getTempDir(), webManager.getSeedUrl(), 
                        protocol+"//"+decode(file.getPath().substring(tempDirLength+1),UTF_8), file.getPath(), resource.prId(), 
                        vsfServicesMap
                    );  
                } catch (Exception e) {
                     writeMessage("The file " + file.getPath().substring(tempDirLength + 1) + 
                                    " could not be added: " + e.getMessage());
                }
                 writeMessage("Processed file " + (++processedFiles) + " of " + files.size() );
            }
            
             writeMessage("Done crawling website. Crawled " + webManager.getCrawledUrls().size() + " URLs.");
            return ResponseEntity.ok(resource);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (webManager != null) {
                webManager.cleanup();
            }
        }
    }

    @Transactional
    @PostMapping("add-project-zip")
    public ResponseEntity<ProjectResource> addZipResource(
        @RequestBody AddZipRequest request
    ) throws Exception {
        int projectId = request.projectId();
        ZipContentManager zipManager = null;
        
        try {
            zipManager = new ZipContentManager();
            zipManager.extractZip(request.zipContent());
            
            // Create project resource
            ProjectResource resource = projectResourceRepository.createResource(
                projectId, request.zipName(), ResTypes.zip, null
            );
            
            // Get vector stores for the project
            Map<Types, RepoVectorStoreResponse> vsMap = CCProjectFileManager.getVectorStoretMap(
                vsRepository.getVectorStoresByProjectId(projectId)
            );
            
            // Create maps for different types of vector stores
            Map<Types, VectorStoreFile> vsfServicesMap = getVsfServicesMap(vsMap);
            int tempDirLength = zipManager.getTempDir().length();
            int processedFiles = 0;
            Set<File> files = zipManager.getAllFiles();
             writeMessage("Found " + files.size() + " files to process.");
            // Process files
            for (File file : files) {
                try {
                    String relFilePath=file.getPath().substring(tempDirLength + 1);
                    if(deleteIfExists(file.getPath().substring(tempDirLength+1), projectId, vsfServicesMap,tempDirLength)) {
                        writeMessage("The file " + file.getPath().substring(tempDirLength + 1) + 
                            " already exists it will be refreshed.");
                    }
                    addFile(zipManager.getTempDir(), "",relFilePath,file.getPath(), resource.prId(), 
                        vsfServicesMap
                    );  
                } catch (Exception e) {
                    writeMessage("The file " + file.getPath().substring(tempDirLength + 1) + 
                                    " could not be added: " + e.getMessage());
                }
                 writeMessage("Processed file " + (++processedFiles) + " of " + files.size() );
            }
            
            writeMessage("Done adding ZIP archive.");
            return ResponseEntity.ok(resource);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (zipManager != null) {
                zipManager.cleanUp();
            }
        }
    }
    @Transactional
    @PostMapping("add-project-repo")
    public ResponseEntity<ProjectResource> addRepoResource(
        @RequestBody AddRepoRequest request
    ) throws Exception {
        int projectId=request.projectId();
        GithubRepoContentManager pfc=new GithubRepoContentManager();
        try{ 
            if(request.repoURL()!=null){
                if(projectResourceRepository.getResourceId(projectId, request.repoURL())!=null){
                    throw new Exception("The repo "+request.repoURL()+" is already added to the project. Use refresh-repo to update it.");
                }
                pfc=new GithubRepoContentManager(request.username(), request.password());
                pfc.addRepository(request.repoURL(), request.branch());
            } else {
                throw new Exception("Repo url is required");
            }
            //Create project resource
            Map<Labels,UserSecret> userSecrets = new HashMap<>();
            if(request.username()!=null && !request.username().isEmpty()){
                userSecrets.put(Labels.username, new UserSecret(Labels.username, request.username()));
                userSecrets.put(Labels.password, new UserSecret(Labels.password, request.password()));
            }
            userSecrets.put(Labels.branch, new UserSecret(Labels.branch, request.branch()));
            userSecrets.put(Labels.commitHash, new UserSecret(Labels.commitHash, pfc.getCommitHash()));
            ProjectResource resource=projectResourceRepository.createResource(
                request.projectId(), request.repoURL(), ResTypes.git, userSecrets
            );
            Map<Types,RepoVectorStoreResponse> vsMap = CCProjectFileManager.getVectorStoretMap(
                vsRepository.getVectorStoresByProjectId(projectId)
            );
            Map<Types, VectorStoreFile> vsfServicesMap = getVsfServicesMap(vsMap);
            int tempDirLength=pfc.getTempDir().length();
            Set<File> files = pfc.getAllFiles();
             writeMessage("Found " + files.size() + " files to process.");
            for (File file : pfc.getAllFiles()) {
                try {
                    addFile(pfc.getTempDir(),pfc.getRootUrl(),
                        pfc.getRootUrl()+file.getPath().substring(tempDirLength + 1),
                        file.getPath(), resource.prId(), vsfServicesMap);
                } catch (Exception e) {
                    writeMessage("The file "+file.getPath().substring(tempDirLength+1)+" could not be added: "+e.getMessage());
                }   
            }
            writeMessage("Done adding new repo.");
            return ResponseEntity.ok(resource);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            pfc.deleteRepository();
        }

    }

    @Transactional
    @PostMapping("{projectId}/refresh-repo")
    public ResponseEntity<Void> refreshRepo(
        @PathVariable int projectId
    ) throws Exception {
        GithubRepoContentManager pfc=new GithubRepoContentManager();
        List<ProjectResource> resources = projectResourceRepository.getResources(projectId);
        Map<Types,RepoVectorStoreResponse> vsMap = CCProjectFileManager.getVectorStoretMap(
            vsRepository.getVectorStoresByProjectId(projectId)
        );
        Map<Types, VectorStoreFile> vsfServicesMap = getVsfServicesMap(vsMap);

        for (ProjectResource resource : resources) {
            if (resource.uri() != null) {
                try {
                    String branch=resource.secrets().get(Labels.branch).value();
                    String oldCommitHash=resource.secrets().get(Labels.commitHash).value();
                    String commitHash=pfc.getLatestCommitHash(resource.uri(), branch);
                    if(commitHash.equals(oldCommitHash)){
                        writeMessage("No changes in the repo "+resource.uri());
                        continue;
                    }
                    GitHubChangeTracker changes=pfc.getChangesSinceCommitViaGitHubAPI( 
                        resource.uri(), oldCommitHash, branch
                    );
                     writeMessage(changes.deletedFiles().size()+" to be deleted.");
                    int deletedFiles=0;
                    for (String deletedFile : changes.deletedFiles()) {
                        try {
                            deleteIfExists( deletedFile, resource.projectId(), vsfServicesMap, pfc.getTempDir().length());
                        } catch (Exception e) {
                            writeMessage("This file is ignored or could not be retrieved: "+deletedFile);
                        }
                         writeMessage("Deleted file " + (++deletedFiles) + " of " + changes.deletedFiles().size() );
                    }

                     writeMessage(changes.addedFiles().size()+" to be added.");
                    int addedFiles=0;
                    for (String addedFile : changes.addedFiles()) {
                        try {
                            addFile(pfc.getTempDir(), pfc.getRootUrl(), 
                                pfc.getRootUrl()+addedFile.substring(pfc.getTempDir().length() + 1), 
                                pfc.getTempDir()+"/"+addedFile, resource.prId(), vsfServicesMap
                            );
                        } catch (Exception e) {
                            writeMessage("The file "+addedFile+" could not be added: "+e.getMessage());
                        }   
                         writeMessage("Added file " + (++addedFiles) + " of " + changes.addedFiles().size() );
                    }
                    writeMessage("Updating commit hash...");
                    projectResourceRepository.updateSecret(resource.prId(), Labels.commitHash, commitHash);
                    writeMessage("Done refreshing repo.");

                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                } finally {
                    pfc.deleteRepository();
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    
    @Transactional
    @PostMapping("create-project")
    public ResponseEntity<Project> createProject(
        @RequestBody CreateProjectRequest request
    ) throws Exception {
        GithubRepoContentManager pfc=new GithubRepoContentManager();
        try{ 
            //Test createAssistant
            // createAssistant("myProject", 24, new LinkedHashMap<>() {{
            //     put("vs_67bfff4b3f808191ab92a49f9c192eab", 42);
            // }});
            int projectId=projectRepository.addProject(request.name(), request.description());
            if(projectId==0){
                throw new Exception("project could not be created.");
            }
            writeMessage("project created with id: "+projectId);
            if(request.repoURL()!=null){
                pfc=new GithubRepoContentManager(request.username(), request.password());
                pfc.addRepository(request.repoURL(), request.branch());
            } else {
                throw new Exception("Repo url is required");
            }
            //Create project resource
            Map<Labels,UserSecret> userSecrets = new HashMap<>();
            if(request.username()!=null && !request.username().isEmpty()){
                userSecrets.put(Labels.username, new UserSecret(Labels.username, request.username()));
                userSecrets.put(Labels.password, new UserSecret(Labels.password, request.password()));
            }
            userSecrets.put(Labels.branch, new UserSecret(Labels.branch, request.branch()));
            userSecrets.put(Labels.commitHash, new UserSecret(Labels.commitHash, pfc.getCommitHash()));
            ProjectResource pr=projectResourceRepository.createResource(projectId, request.repoURL(), ResTypes.git, userSecrets);
            Map<Types,VectorStore> vsMap = createEmptyVectorStores(projectId);
            Map<Types, VectorStoreFile> vsfServicesMap = getVsfServicesMapFormVsMap(vsMap);
            Set<File> files = pfc.getAllFiles();
             writeMessage("Found " + files.size() + " files to process.");
            int processedFiles = 0;
            for(File file : files) {
                try {
                    addFile(pfc.getTempDir(), pfc.getRootUrl(), 
                        pfc.getRootUrl()+file.getPath().substring(pfc.getTempDir().length()+1), 
                        file.getPath(), pr.prId(), vsfServicesMap);
                } catch (Exception e) {
                    writeMessage("The file "+file.getPath()+" could not be added: "+e.getMessage());
                }   
                writeMessage("Processed file " + (++processedFiles) + " of " + files.size() );
            }
            writeMessage("Create assistant...");
            int assistantId=createAssistant(request.name(), projectId, vsMap);
            writeMessage("Assistant created with id: "+assistantId);
            Project project = new Project(projectId, request.name(), request.description(), this.getCurrentUserId(), assistantId);
            return ResponseEntity.ok(project);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            pfc.deleteRepository();
        }
    }
    /**
     * Create an assistant for the project
     * @param name the name of the assistant
     * @param projectId the id of the project
     * @param vectorStoreMap the map of vector stores
     * @return the id of the assistant
     * @throws IOException
     */
    private int createAssistant(
        String name, int projectId, Map<Types,VectorStore> vectorStoreMap
    ) throws IOException {
        Models model=Models.gpt_4o;
        AssistantBuilder assistantBuilder = new AssistantBuilder(model);
        String instruction="""
            <Function: You are a code search assistant designed to help users analyze and understand their projects. Your primary role is to provide detailed explanations, code snippets, and actionable suggestions based on the project's files and metadata.>

            Always respond in the following structured JSON format, and do not prefix with ```<language>:
            {
                "answers": [
                    {
                        "explanation": "<Detailed explanation>",
                        "language": "<Programming language (if applicable)>",
                        "code": "<Formatted code snippet (if applicable)>",
                        "codeExplanation": "<Explanation of the code snippet (if applicable)>",
                        "references": ["<Relevant sources>"]
                    }
                    // Add more answers as needed
                ],
                "conversationalGuidance": "<Additional guidance for the user: Intelligent Follow-ups, Actionable Suggestions, Engagement & Clarifications, etc.>"
            }

            Markdown is supported in the explanation, code explanation, and reference fields.
            Never use the internal citation markers in response.
            Do not use bullet characters in the response.

            ### File Metadata Usage
            When analyzing files, use the following attributes from the file metadata to provide insights and context:
            - **`name`**: Use the file name to identify the file and provide context in your response.
            - **`fileUrl`**: Use the file's fileUrl to locate it within the project and reference it in your response.
            - **`extension`**: Use the file extension to determine the programming language or file type (e.g., `java` for Java, `py` for Python).
            - **`mime-type`**: Use the MIME type to understand the file's format or content type (e.g., `text/plain`, `application/json`).
            - **`nbLines`**: Use the number of lines in the file to assess its size or complexity. For example:
            - Small files (e.g., <50 lines) may be utility scripts or configuration files.
            - Large files (e.g., >500 lines) may indicate complex logic or large datasets.
            - **`type`**: Use the file type (e.g., `code`, `markup`, `config`) to tailor your analysis and suggestions. For example:
            - For `code` files, focus on programming logic, structure, and potential improvements.
            - For `markup` files, focus on formatting, structure, and content organization.
            - For `config` files, focus on configuration correctness and best practices.

            ### Analyzing Files
            - Use the `extension` and `mime-type` attributes to determine the programming language or file type. For example:
            - `java` → Java
            - `py` → Python
            - `html` → HTML
            - Use the `nbLines` attribute to assess the file's complexity and provide insights. For example:
            - "This file contains 120 lines of Java code, which suggests it implements a moderately complex class."
            - Use the `type` attribute to guide your analysis. For example:
            - For `code` files, analyze the logic, structure, and potential improvements.
            - For `markup` files, analyze the formatting and content organization.
            - For `config` files, analyze the correctness and adherence to best practices.

            ### Referencing Files
            - Do not use the internal name or path, always use file vector attributes such as `name` and `fileUrl` when referencing specific files.
            - Use the `nbLines` attribute to provide insights into the file's size or complexity when relevant.
            - Use the `mime-type` attribute to describe the file's format or content type.
            - When retrieving code, always reference the file's attributes `fileUrl` and `name` to provide context.

            #### Markdown Links for References
            - Use Markdown links with a title attribute to reference files. For example example with attributes (name=CodechatController.java, fileUrl=https://github.com/benoitmarsot/codechat/blob/main/src/main/java/com/unbumpkin/codechat/controller/CodechatController.java):
            `[CodechatController.java](https://github.com/benoitmarsot/codechat/blob/main/src/main/java/com/unbumpkin/codechat/controller/CodechatController.java "Java source file")`.

            ### Handling Non-Code Queries
            - If the query is not related to code, omit the `language` and `code` fields in the response. Focus on providing a clear explanation and actionable suggestions.

            ### Example Response
            {
                "answers": [
                    {
                        "explanation": "The file `MyClass.java` contains the implementation of the main application logic. It is located at `src/main/java/com/example/MyClass.java` and contains 120 lines of Java code. The file's MIME type is `text/x-java-source`.",
                        "language": "Java",
                        "code": "public class MyClass { ... }",
                        "codeExplanation": "This code defines the main class of the application.",
                        "references": ["[CodechatController.java](https://github.com/benoitmarsot/codechat/blob/main/src/main/java/com/unbumpkin/codechat/controller/CodechatController.java \"Java source file\")"]
                    }
                ],
                "conversationalGuidance": "Would you like to see more details about this file or related files?"
            }
            ### Example Response with no code
            {
                "answers": [
                    {
                        "explanation": "The file `MyClass.java` contains the implementation of the main application logic. It is located at `src/main/java/com/example/MyClass.java` and contains 120 lines of Java code. The file's MIME type is `text/x-java-source`.",
                        "references": ["[CodechatController.java](https://github.com/benoitmarsot/codechat/blob/main/src/main/java/com/unbumpkin/codechat/controller/CodechatController.java \"Java source file\")"]
                    }
                ],
                "conversationalGuidance": "Would you like to see more details about this file or related files?"
            }
            """;        
        assistantBuilder.setName(name)
            .setDescription("Code search assistant for " + name)
            .setInstructions(instruction).setReasoningEffort(ReasoningEfforts.high)
            // to use the codechat_assistant_response.json schema
            //.addOutsideJsonSchemaResponseFormat()
            //.setTemperature(.02) //Not suported in o3-mini
            .addFileSearchTool().addFileSearchAssist()
            .setFileSearchMaxNumResults(20) //default
            //.setFileSearchRankingOption(.5) 
            .setToolResourcesFileSearch(Set.of(vectorStoreMap.get(Types.all).getOaiVsid())) //: can only put one vs so putting vsAll 
            //Function are not needed since we use attributes metadata
            // .addFunction()
            //     .setFunctionName("countLines")
            //     .setFunctionDescription("This function will return the number of lines in a file")
            //     .FunctionAddParameter("fileid", "string", "The id of the file")
            //todo: implement get the file name from the file_id
            // .addFunction()
            //     .setFunctionName("getFilename")
            //     .setFunctionDescription("This function will return the name of file")
            //     .FunctionAddParameter("fileid", "string", "The id of the file");
            // .addFunction()
            //     .setFunctionName("isAnswerCode")
            //     .setFunctionDescription("This function will return the name of file")
            //     .FunctionAddParameter("fileid", "string", "The id of the file");
            ;
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // String responseTemplate=JsonUtils.loadJson( "json_schema/codechat_assistant_response.json");
        // String assistantJson = mapper.writeValueAsString(assistantBuilder).replace("\"{json_schema}\"", responseTemplate);
        // System.out.println(assistantJson);
        System.out.println(mapper.writeValueAsString(assistantBuilder));

        // Uses the codechat_assistant_response.json schema for the response
        // - put back: .addOutsideJsonSchemaResponseFormat()
        //String assistantOaiId=assistantService.createAssistant(assistantJson);
        // Uses the json example in the instruction for the response
        String assistantOaiId=assistantService.createAssistant(assistantBuilder);
        Assistant assistant = new Assistant(
            0, assistantOaiId, projectId, name, "Code search assistant for " + name,
            instruction, ReasoningEfforts.high, model, .7f, 10, 
            vectorStoreMap.get(Types.code).getVsid(), vectorStoreMap.get(Types.markup).getVsid(), 
            vectorStoreMap.get(Types.config).getVsid(), vectorStoreMap.get(Types.all).getVsid()
        );
        return assistantRepository.addAssistant(assistant);
    }
    private Map<Types,VectorStore> createEmptyVectorStores(
        int projectId
    ) throws IOException {
        Map<Types,VectorStore> vectorStoreMap = new LinkedHashMap<>(4);
        for(Types type : Types.values()) {
            if(type==Types.image||type==Types.social){
                continue;
            }
            String vsName = "vs"+type.name();
            String vsDesc = "contain the "+type.name()+" files in the project.";
            VectorStore vs = new VectorStore( 0,"", projectId,vsName,vsDesc, null,type);
            String vsOaiId = vsService.createVectorStore(vs);
            vs = new VectorStore(0, vsOaiId, projectId, vsName, vsDesc, null, type);
            vs = new VectorStore( vsRepository.storeVectorStore(vs),
                vsOaiId, projectId, vsName, vsDesc, null, type);
            vectorStoreMap.put(type,vs );
             writeMessage("Empty vector store "+type.name()+" created with id: "+vs.getVsid()+" and OaiId: "+vsOaiId);
        }
        return vectorStoreMap;
    }

    /**
     * Get the map of vector store files services
     * @param vsMap the map of vector stores
     * @return the map of vector store files services
     */
    private Map<Types, VectorStoreFile> getVsfServicesMap( Map<Types, RepoVectorStoreResponse> vsMap) {
        Map<Types, VectorStoreFile> vsfServicesMap = new HashMap<>(4);
        for(Types type:Types.values()){
            if(type!=Types.image&&type!=Types.social){
                vsfServicesMap.put(type, new VectorStoreFile(vsMap.get(type).vsid()));
            }
        }
        return vsfServicesMap;
    }
    private Map<Types, VectorStoreFile> getVsfServicesMapFormVsMap( Map<Types, VectorStore> vsMap) {
        Map<Types, VectorStoreFile> vsfServicesMap = new HashMap<>(4);
        for(Types type:Types.values()){
            if(type==Types.image||type==Types.social){
                continue;
            }
            vsfServicesMap.put(type, new VectorStoreFile(vsMap.get(type).getOaiVsid()));
        }
        return vsfServicesMap;
    }

    /**
     * Delete the file if it exists in the database and remove it from the vector store.
     * @param filePath The oaifilepath
     * @param prId the project resource id
     * @param vsfServicesMap the map of vector store files services
     * @param vsfServicesAll the vector store file service for all files
     * @return true if the file was deleted, false otherwise
     * @throws IOException
     */
    private boolean deleteIfExists( String filePath, int projectId, 
        Map<Types,VectorStoreFile> vsfServicesMap, int rootPathLength
    ) throws IOException {        
        List<OaiFile> files = oaiFileRepository.getOaiFileByPath(filePath, projectId);
        boolean wasDeleted=false;
        for (OaiFile oaiFile : files) {
            Types fileType = getFileType(filePath);
            if(fileType!=Types.image&&fileType!=Types.social){
                vsfServicesMap.get(fileType).removeFile(oaiFile.fileId());
            }
            vsfServicesMap.get(Types.all).removeFile(oaiFile.fileId());
            oaiFileService.deleteFile(oaiFile.fileId());
            oaiFileRepository.deleteFile(oaiFile.fileId());
           writeMessage(oaiFile.filePath()+" id "+oaiFile.fileId()+" removed from all and "+fileType.toString()+" vector stores and deleted.");
            wasDeleted=true;
        }
        return wasDeleted;
    }
    private void addFile(String tempDirPath, String rootDirUrl, String fileUrl, String filePath, int prId,
        Map<Types,VectorStoreFile> vsfServicesMap
    ) throws IOException {
        File file = new File(filePath);

        int tempDirLength=tempDirPath.length();
        Types fileType=getFileType(file);
        FileRenameDescriptor desc;
        try {
            desc = getFileRenameDescriptor(file,fileType);
        } catch (Exception e) {
           writeMessage("The file "+file.getPath().substring(tempDirLength+1)+" could not be added: "+e.getMessage());
            return;
        }
        OaiFile oaiFile = oaiFileService.uploadFile(desc.newFile().getAbsolutePath(), tempDirLength+1, Purposes.assistants, prId);
       writeMessage("file "+file.getName()+" uploaded with id "+oaiFile.fileId());
        CreateVSFileRequest request = getCreateVSFileRequest( desc, rootDirUrl, oaiFile, tempDirLength);
        if(fileType!=Types.image&&fileType!=Types.social){
            vsfServicesMap.get(fileType).addFile( request);
        }

        vsfServicesMap.get(Types.all).addFile( request);
        String relFilePath=oaiFile.rootdir()+(oaiFile.rootdir().isEmpty()?"":"/")+desc.oldFileName();
        oaiFile= new OaiFile(
            oaiFile.fId(), oaiFile.prId(), oaiFile.fileId(), request.attributes().get("name"), rootDirUrl, 
            relFilePath,oaiFile.purpose(), oaiFile.linecount()
        );
        oaiFileRepository.storeOaiFile(oaiFile, oaiFile.prId());

      writeMessage("File id "+oaiFile.fileId()+" added to "+fileType.toString()+" vector store ");
    }
    private CreateVSFileRequest getCreateVSFileRequest( 
        FileRenameDescriptor desc, String fileUrl, OaiFile oaiFile, int tempDirLength
    ) throws IOException {
        String oldExt = FileUtils.getFileExtension(desc.oldFileName());
        Types fileType=getFileType(desc.oldFileName());
         writeMessage("URL:" + fileUrl);
        CreateVSFileRequest creaVsRequest = new CreateVSFileRequest(
            oaiFile.fileId(), new HashMap<>() {{
                put("name", desc.oldFileName());
                put("fileUrl", fileUrl);
                put("extension", oldExt);
                put("mime-type", ExtMimeType.getMimeType(oldExt));
                put("nbLines", String.valueOf(FileUtils.countLines(desc.newFile())));
                put("type", fileType.name());
            }}
        );
        return creaVsRequest;
    }

    private FileRenameDescriptor getFileRenameDescriptor(File file, Types fileType) throws Exception {
        if(fileType==Types.image){
            try {
                String imgDesc=responsesService.describeImage(
                    new OaiImageDescResponsesRequest(
                        Models.gpt_4o_mini, 
                        "Describe the given image in a short and clear way. List key objects, the overall scene, and provide a lot of relevant tags.", 
                        file, Details.low
                    ), true
                );
                if(imgDesc==null || imgDesc.isEmpty()){
                    throw new Exception("The image description is empty.");
                }
                String oldFileName= file.getName();
                String oldFilePath=file.getAbsolutePath();
                String ext = FileUtils.getFileExtension(file);

                file = new File(file.getAbsolutePath()+".txt");
                // Write the image description to the text file
                try (FileWriter writer = new java.io.FileWriter(file)) {
                    writer.write(imgDesc);
                }
               writeMessage(format(
                    "%s image description: \n\t%s", oldFileName, imgDesc)
                );
                return new FileRenameDescriptor(
                    oldFileName, oldFilePath,file,getMimeType(ext)
                );
            } catch (Exception e) {
                throw new Exception("Error reading or describing the image: " + e.getMessage());
                
            }
        } 
        return ExtMimeType.oaiRename(file);
    }
    
    @GetMapping("/debug")
    public SseEmitter streamDebugMessages(HttpServletResponse response) {
        // Set custom headers for the response
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Content-Type", "text/event-stream");
    
        emitter = new SseEmitter(TIME_OUT_SSL);
    
        emitter.onCompletion(() -> {
            System.out.println("SSE stream completed.");
            emitter = null; // Reset emitter when completed
        });
    
        emitter.onTimeout(() -> {
            System.out.println("SSE stream timed out.");
            emitter.complete();
            emitter = null; // Reset emitter on timeout
        });
    
        emitter.onError((e) -> {
            String errorMessage = (e != null) ? e.getMessage() : "Unknown error occurred";
            System.out.println("SSE stream error: " + errorMessage);
            if (emitter != null) {
                emitter.completeWithError(e);
            }
            emitter = null; // Reset emitter on error
        });
    
        writeMessage("Starting server communication...");
        return emitter;
    }

    private void writeMessage(String message) {
        writeMessage(message, 1);
    }
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    //todo @PreDestroy
    //public void shutdownExecutor() {
    //  executorService.shutdown();}
    
    private void writeMessage(String message, int priority) {
        if (emitter != null) {
            executorService.submit(() -> {
                try {
                    if (priority <= PRIORITY_MESSAGE_LIMIT) {
                        emitter.send(SseEmitter.event()
                                .name("debug")
                                .data(message));
                    }
                } catch (IllegalStateException e) {
                    System.out.println("Emitter is already completed: " + e.getMessage());
                    emitter = null; // Reset emitter to avoid further errors
                } catch (IOException e) {
                    System.out.println("Error sending SSE message: " + e.getMessage());
                    emitter.completeWithError(e);
                    emitter = null; // Reset emitter on error
                }
            });
        } else {
            System.out.println(message);
        }
    }

    @GetMapping("/debug/stop")
    public ResponseEntity<String> stopDebugMessages() {
        emitter = null;
        return ResponseEntity.ok("Debug message stream stopped.");
    }
   

    @GetMapping("/testdebug")
    public SseEmitter streamTestMessages(HttpServletResponse response) {
        if(emitter == null)
            streamDebugMessages(response);

        // Start a new thread to send debug messages to the client
        executorService.submit(() -> {
            try {
                while (emitter != null) {
                    DebugMessage debugMessage = new DebugMessage("This is a debug message from CodechatController", java.sql.Timestamp.valueOf(LocalDateTime.now()), 1);
                    emitter.send(SseEmitter.event()
                            .name("debug")
                            .data(debugMessage));
                    // Sleep for 1 second
                    Thread.sleep(1000);
                }
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}

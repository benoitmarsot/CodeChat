package com.unbumpkin.codechat.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.lang.String.format;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
import com.unbumpkin.codechat.service.openai.ZipContentManager;
import com.unbumpkin.codechat.util.ExtMimeType;
import com.unbumpkin.codechat.util.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.unbumpkin.codechat.dto.FileRenameDescriptor;
import com.unbumpkin.codechat.dto.GitHubChangeTracker;
import com.unbumpkin.codechat.dto.openai.Assistant;
import com.unbumpkin.codechat.dto.request.AddRepoRequest;
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
import com.unbumpkin.codechat.model.openai.OaiFile.Purposes;
import com.unbumpkin.codechat.model.openai.VectorStore;
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
import com.unbumpkin.codechat.service.openai.AssistantBuilder;
import com.unbumpkin.codechat.service.openai.AssistantBuilder.ReasoningEfforts;
import com.unbumpkin.codechat.service.openai.AssistantService;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;
import com.unbumpkin.codechat.service.openai.CCProjectFileManager;
import com.unbumpkin.codechat.service.openai.CCProjectFileManager.Types;
import static com.unbumpkin.codechat.service.openai.CCProjectFileManager.getFileType;
import com.unbumpkin.codechat.service.openai.GithubRepoContentManager;
import com.unbumpkin.codechat.service.openai.OaiFileService;
import com.unbumpkin.codechat.service.openai.VectorStoreFile;
import com.unbumpkin.codechat.service.openai.VectorStoreService;
import com.unbumpkin.codechat.util.ExtMimeType;
import com.unbumpkin.codechat.util.FileUtils;



@RestController
@RequestMapping("/api/v1/codechat")
public class CodechatController {
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
        System.out.println("project created with id: "+projectId);

        Map<Types,VectorStore> vectorStoreMap = createEmptyVectorStores(projectId);
        System.out.println("Create assistant...");
        int assistantId=createAssistant(request.name(), projectId, vectorStoreMap);
        System.out.println("Assistant created with id: "+assistantId);
        Project project = new Project(projectId, request.name(), request.description(), this.getCurrentUserId(), assistantId);
        return ResponseEntity.ok(project);
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
            
            // Process files
            for (File file : zipManager.getAllFiles()) {
                try {
                    if(deleteIfExists(file.getPath().substring(tempDirLength+1), projectId, vsfServicesMap,tempDirLength)) {
                        System.out.println("The file " + file.getPath().substring(tempDirLength + 1) + 
                            " already exists it will be refreshed.");
                    }
                    addFile(zipManager.getTempDir(), "",file.getPath(), resource.prId(), 
                        vsfServicesMap
                    );  
                } catch (Exception e) {
                    System.out.println("The file " + file.getPath().substring(tempDirLength + 1) + 
                                    " could not be added: " + e.getMessage());
                }
            }
            
            System.out.println("Done adding ZIP archive.");
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
            for (File file : pfc.getAllFiles()) {
                try {
                    addFile(pfc.getTempDir(),pfc.getRootUrl(),file.getPath(), resource.prId(), vsfServicesMap);
                } catch (Exception e) {
                    System.out.println("The file "+file.getPath().substring(tempDirLength+1)+" could not be added: "+e.getMessage());
                }   
            }
            System.out.println("Done adding new repo.");
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
                        System.out.println("No changes in the repo "+resource.uri());
                        continue;
                    }
                    GitHubChangeTracker changes=pfc.getChangesSinceCommitViaGitHubAPI( 
                        resource.uri(), oldCommitHash, branch
                    );
                    for (String deletedFile : changes.deletedFiles()) {
                        try {
                            deleteIfExists( deletedFile, resource.projectId(), vsfServicesMap, pfc.getTempDir().length());
                        } catch (Exception e) {
                            System.out.println("This file is ignored or could not be retrieved: "+deletedFile);
                        }
                    }

                    for (String addedFile : changes.addedFiles()) {
                        try {
                            addFile(pfc.getTempDir(), pfc.getRootUrl(), pfc.getTempDir()+"/"+addedFile, resource.prId(), vsfServicesMap);
                        } catch (Exception e) {
                            System.out.println("The file "+addedFile+" could not be added: "+e.getMessage());
                        }   
                    }
                    System.out.println("Updating commit hash...");
                    projectResourceRepository.updateSecret(resource.prId(), Labels.commitHash, commitHash);
                    System.out.println("Done refreshing repo.");

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

    
    @PostMapping("create-project-from-url")
    public ResponseEntity<String> createProjectFromUrl(
        @RequestBody CreateProjectRequest request
    ) throws Exception {
        String url = request.sourcePath();
        if (url == null) {
            throw new Exception("Source url required");
        }
        int projectId = projectRepository.addProject(request.name(), request.description());
        if (projectId == 0) {
            throw new Exception("project could not be created.");
        }
        System.out.println("project created with id: " + projectId);
        //Create project resource
        Map<Labels,UserSecret> userSecrets = new HashMap<>();
        if(request.username()!=null && !request.username().isEmpty()){
            userSecrets.put(Labels.username, new UserSecret(Labels.username, request.username()));
            userSecrets.put(Labels.password, new UserSecret(Labels.password, request.password()));
        }
        ProjectResource pr=projectResourceRepository.createResource(projectId, request.sourcePath(), userSecrets);
        System.out.println("Project resource created with id: " + pr.prId());

        Map<String,Integer> vectorStorMap = new LinkedHashMap<>();
        Map<String,CreateVSFileRequest> allFileIds = new HashMap<>();
        File tempDir = Files.createTempDirectory("website-crawl-").toFile();

        System.out.println("Beginning crawl of " + url);
        Integer maxDepth = request.maxSearchDepth();
        CCProjectFileManager pfc = crawlWebsite(url, (maxDepth != null) ? maxDepth : 2, request.urlIncludes(), tempDir.getAbsolutePath());
        System.out.println("Done crawling");

        System.out.println("Uploading and create vector store for code files...");
        createVectorStore(pfc, pr.prId(), projectId, "vsMarkup", Types.code, allFileIds, vectorStorMap, tempDir.getAbsolutePath().length());
        System.out.println("Uploading and create vector store for markup files...");
        createVectorStore(pfc, pr.prId(), projectId, "vsMarkup", Types.markup, allFileIds, vectorStorMap, tempDir.getAbsolutePath().length());
        System.out.println("Uploading and create vector store for config files...");
        createVectorStore(pfc, pr.prId(), projectId, "vsMarkup", Types.config, allFileIds, vectorStorMap, tempDir.getAbsolutePath().length());

        System.out.println("Create vector store for all files...");
        String vsAllOaiId = vsService.createVectorStore(
            new VectorStore("vsAll", "contain all the files in the project.", null, null, null, null)
        );
        int vsAllId = vsRepository.storeVectorStore(
            new VectorStore(0, vsAllOaiId, projectId, "vsAll", "contain all the files in the project.", null, Types.all)
        );
        VectorStoreFile vsfService = new VectorStoreFile(vsAllOaiId);
        for (String oaiFileId : allFileIds.keySet()) {
            vsfService.addFile(allFileIds.get(oaiFileId));
            System.out.println("File id " + oaiFileId + " added to global vector store " + vsAllOaiId);
        }
        vectorStorMap.put(vsAllOaiId, vsAllId);
        
        System.out.println("Create assistant...");
        int assistantId = createAssistant(request.name(), projectId, vectorStorMap, vsAllOaiId);
        System.out.println("Assistant created with id: " + assistantId);

        deleteDirectoryRecursively(tempDir);
        return ResponseEntity.ok("Ok");
    }
    private CCProjectFileManager crawlWebsite(
        String startUrl, int maxDepth, String urlIncludes, String tempDirPath
    ) throws IOException {
        CCProjectFileManager pfc = new CCProjectFileManager();
        try (
            Playwright playwright = Playwright.create(); 
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        ) {

            Page page = browser.newPage();
            Set<String> visited = new HashSet<>();
            Queue<PageNode> nodes = new LinkedList<>();
            int fileNum = 0;
            nodes.add(new PageNode(startUrl, 0));
            
            while (!nodes.isEmpty()) {
                PageNode currentPage = nodes.poll();
                String currentUrl = currentPage.url;
                if (visited.contains(currentUrl)) {
                    continue;
                }
                visited.add(currentUrl);
                fileNum++;
                try {
                    page.navigate(currentUrl, new Page.NavigateOptions().setTimeout(20000));
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                } catch (Exception e) {
                    System.err.println("Error navigating to " + currentUrl + ": " + e.getMessage());
                    continue;
                }
                
                String text = page.innerText("body");
                String safeTitle = page.title().replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + fileNum;
                String filePath = tempDirPath + safeTitle + ".html";
                try {
                    Files.write(Paths.get(filePath), String.join("\n", text).getBytes());
                    System.out.println("Successfully wrote to " + filePath);
                } catch (IOException e) {
                    System.err.println("Error writing to file or uploading to vector store: " + e.getMessage());
                }
                pfc.addFile(new File(filePath));

                if (currentPage.depth >= maxDepth-1) {
                    continue;
                }
                List<ElementHandle> anchorElements = page.querySelectorAll("a[href]");
                for (ElementHandle element : anchorElements) {
                    String nextUrl = element.getAttribute("href");
                    if (nextUrl != null && !nextUrl.startsWith("http")) {
                        nextUrl = currentUrl + nextUrl;
                    }
                    boolean validUrl = nextUrl != null && nextUrl.startsWith("http") && (urlIncludes == null || nextUrl.contains(urlIncludes));
                    if (validUrl && !visited.contains(nextUrl)) {
                        nodes.add(new PageNode(nextUrl, currentPage.depth + 1));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pfc;
    }
    static class PageNode {
        String url;
        int depth;
        PageNode(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }
    private void deleteDirectoryRecursively(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file); // Recursively delete subdirectories
                } else {
                    file.delete(); // Delete files
                }
            }
        }
        directory.delete(); // Delete the empty directory
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
            System.out.println("project created with id: "+projectId);
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
            for(File file : pfc.getAllFiles()) {
                try {
                    addFile(pfc.getTempDir(), pfc.getRootUrl(), file.getPath(), pr.prId(), vsfServicesMap);
                } catch (Exception e) {
                    System.out.println("The file "+file.getPath()+" could not be added: "+e.getMessage());
                }   
            }
            System.out.println("Create assistant...");
            int assistantId=createAssistant(request.name(), projectId, vsMap);
            System.out.println("Assistant created with id: "+assistantId);
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
            System.out.println("Empty vector store "+type.name()+" created with id: "+vs.getVsid()+" and OaiId: "+vsOaiId);
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
            System.out.println(oaiFile.filePath()+" id "+oaiFile.fileId()+" removed from all and "+fileType.toString()+" vector stores and deleted.");
            wasDeleted=true;
        }
        return wasDeleted;
    }
    private void addFile(String tempDirPath, String rootDirUrl, String filePath, int prId,
        Map<Types,VectorStoreFile> vsfServicesMap
    ) throws IOException {
        File file = new File(filePath);

        int tempDirLength=tempDirPath.length();
        Types fileType=getFileType(file);
        FileRenameDescriptor desc;
        try {
            desc = getFileRenameDescriptor(file,fileType);
        } catch (Exception e) {
            System.out.println("The file "+file.getPath().substring(tempDirLength+1)+" could not be added: "+e.getMessage());
            return;
        }
        OaiFile oaiFile = oaiFileService.uploadFile(desc.newFile().getAbsolutePath(), tempDirLength+1, Purposes.assistants, prId);
        System.out.println("file "+file.getName()+" uploaded with id "+oaiFile.fileId());
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

       System.out.println("File id "+oaiFile.fileId()+" added to "+fileType.toString()+" vector store ");
    }
    private CreateVSFileRequest getCreateVSFileRequest( 
        FileRenameDescriptor desc, String rootUrl, OaiFile oaiFile, int tempDirLength
    ) throws IOException {
        String oldExt = FileUtils.getFileExtension(desc.oldFileName());
        Types fileType=getFileType(desc.oldFileName());
        String fileUrl=rootUrl+desc.oldFilePath().substring(tempDirLength + 1);
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
                System.out.println(format(
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
}
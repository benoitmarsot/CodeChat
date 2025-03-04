package com.unbumpkin.codechat.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.unbumpkin.codechat.service.openai.AssistantBuilder;
import com.unbumpkin.codechat.service.openai.AssistantService;
import com.unbumpkin.codechat.service.openai.OaiFileService;
import com.unbumpkin.codechat.service.openai.AssistantBuilder.ReasoningEffort;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;
import com.unbumpkin.codechat.service.openai.ProjectFileCategorizer;
import com.unbumpkin.codechat.service.openai.ProjectFileCategorizer.Types;
import com.unbumpkin.codechat.service.openai.VectorStoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.unbumpkin.codechat.domain.Message;
import com.unbumpkin.codechat.domain.Project;
import com.unbumpkin.codechat.domain.openai.Assistant;
import com.unbumpkin.codechat.domain.openai.OaiFile;
import com.unbumpkin.codechat.domain.openai.VectorStore;
import com.unbumpkin.codechat.domain.openai.OaiFile.Purposes;
import com.unbumpkin.codechat.dto.codechat.CreateProjectRequest;
import com.unbumpkin.codechat.repository.DiscussionRepository;
import com.unbumpkin.codechat.repository.MessageRepository;
import com.unbumpkin.codechat.repository.ProjectRepository;
import com.unbumpkin.codechat.repository.openai.AssistantRepository;
import com.unbumpkin.codechat.repository.openai.OaiFileRepository;
import com.unbumpkin.codechat.repository.openai.OaiThreadRepository;
import com.unbumpkin.codechat.repository.openai.VectorStoreRepository;
import com.unbumpkin.codechat.security.CustomAuthentication;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


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
    @Autowired DiscussionRepository discussionRepository;

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
        return new ResponseEntity<String>("All data deleted", HttpStatus.OK);
    }

    
    @PostMapping("create-project")
    public ResponseEntity<Project> createProject(
        @RequestBody CreateProjectRequest request
    ) throws Exception {
        //Test createAssistant
        // createAssistant("myProject", 24, new LinkedHashMap<>() {{
        //     put("vs_67bfff4b3f808191ab92a49f9c192eab", 42);
        // }});
        int projectId=projectRepository.addProject(request.name(), request.description());
        if(projectId==0){
            throw new Exception("project could not be created.");
        }
        System.out.println("project created with id: "+projectId);
        System.out.println("Scanning source path: "+request.sourcePath());
        ProjectFileCategorizer pfc=new ProjectFileCategorizer();
        pfc.addDir(request.sourcePath());
        Map<String,Integer> vectorStorMap = new LinkedHashMap<>();
        List<String> allFileIds = new ArrayList<>();
        //Here the order is important because the assistant will use the vector stores in this order
        // Code, Markup, then Config
        System.out.println("Uploading and create vector store for code files...");
        createVectorStore(pfc, projectId, "vsCode", Types.code, allFileIds, vectorStorMap);
        // System.out.println("Uploading and create vector store for markup files...");
        // createVectorStore(pfc, projectId, "vsMarkup", Types.markup, allFileIds, vectorStorMap);
        // System.out.println("Uploading and create vector store for config files...");
        // createVectorStore(pfc, projectId, "vsConfig", Types.config, allFileIds, vectorStorMap);
        // System.out.println("Create vector store for all files...");
        // String vsAllOaiId=vsService.createVectorStore(
        //     new VectorStore("vsAll","contain all the files in the project.", 
        //         allFileIds,null,null,null)
        // );
        // int vsAllId=vsRepository.storeVectorStore(
        //     new VectorStore(0, vsAllOaiId, "vsAll", 
        //         "contain all the files in the project.", null, Types.all)
        // );
        // vectorStorMap.put(vsAllOaiId, vsAllId);
        System.out.println("Create assistant...");
        int assistantId=createAssistant(request.name(), projectId, vectorStorMap);
        System.out.println("Assistant created with id: "+assistantId);
        Project project = new Project(projectId, request.name(), request.description(), this.getCurrentUserId(), assistantId);
        return new ResponseEntity<Project>(project, HttpStatus.OK);
    }
    private void createVectorStore(
        ProjectFileCategorizer pfc, int projectId, String vsName, Types type,
        List<String> allFileIds, Map<String,Integer> vectorStorMap
    ) throws IOException {
        List<OaiFile> lFiles = new ArrayList<>();
        List<String> lFileIds = new ArrayList<>();
        for (File file : pfc.getFileSetMap(type)) {
            OaiFile oaiFile = oaiFileService.uploadFile(file.getAbsolutePath(), Purposes.assistants, projectId);
            lFiles.add(
                oaiFile
            );
            lFileIds.add(oaiFile.fileId());
            allFileIds.add(oaiFile.fileId());
        }
        oaiFileRepository.storeOaiFiles(lFiles, projectId);
        String vsOaiId = vsService.createVectorStore(
            new VectorStore(vsName,"contain the "+type.name()+" files in the project.", 
                lFileIds,null,null,null)
        );
        //(int vsId, String oaiVsId, String vsname, String vsdesc, Instant created, Integer dayskeep, Types type)
        VectorStore vs = new VectorStore(0, vsOaiId, vsName, 
            "contain the "+type.name()+" files in the project.", null, type);
        int vsId=vsRepository.storeVectorStore(vs);
        vsRepository.addFiles(vsOaiId, lFileIds);
        vectorStorMap.put(vsOaiId, vsId);
    }
    private int createAssistant(
        String name, int projectId, Map<String,Integer> vectorStorMap
    ) throws IOException {
        AssistantBuilder assistantBuilder = new AssistantBuilder(Models.gpt_4_turbo);

        assistantBuilder.setName(name)
            .setDescription("Code search assistant for " + name)
            .setInstructions("""
            You are experienced an software enginer. 
            When asked a question, analyse using the your code, config, markup vector stores
            to answer using a deep knowledge of the project at hands.
            You may also write code that fit with the style and the architectur of the project.
            
            Always respond in the following structured JSON format:
            {
            "question": "<question of the user>",
            "answers": [
                {
                "explanation": "<Detailed explanation>",
                "language": "<Programming language (if applicable)>",
                "code": "<Formatted code snippet (if applicable)>",
                "references": ["<Relevant sources>"]
                }
                // Add more answer objects as needed
            ]

            Ensure the response is always valid JSON. If the query of the user is not code-related, omit the language and code fields.
            """).setReasoningEffort(ReasoningEffort.high)
            .setTemperature(.02)
            .addFileSearchTool().addFileSearchAssist()
            .setFileSearchMaxNumResults(20) //default
            //.setFileSearchRankingOption(.5) 
            .setToolResourcesFileSearch(vectorStorMap.keySet()) //: can only put one vs so putting vsCode 
            //todo: implement get the number of lines from the file_id
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
        String assistantJson = mapper.writeValueAsString(assistantBuilder);
        System.out.println(assistantJson);
        mapper.writeValueAsString(assistantBuilder);
        String assistantOaiId=assistantService.createAssistant(assistantBuilder);
        Integer[] vsIds = vectorStorMap.values().toArray(new Integer[0]);
        Assistant assistant = new Assistant(0, assistantOaiId, name, "Code search assistant for " + name,
            //For now submit code 4 times
            projectId, vsIds[0], vsIds[0], vsIds[0], vsIds[0]
        );
        return assistantRepository.addAssistant(assistant);
    }

}
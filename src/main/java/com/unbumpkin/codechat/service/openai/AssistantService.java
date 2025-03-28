package com.unbumpkin.codechat.service.openai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.dto.request.ModifyAssistantRequest;
import com.unbumpkin.codechat.service.openai.AssistantBuilder.ReasoningEfforts;

import okhttp3.Request;
import okhttp3.RequestBody;

@Service
public class AssistantService extends BaseOpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/assistants";
    public enum AssistantTools {
        code_interpreter,
        file_search,
        function
    }
    public String createAssistant(AssistantBuilder helper) throws IOException {
        String json = objectMapper.writeValueAsString(helper);
        //System.out.println(json);
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("OpenAI-Beta", "assistants=v2")
            .addHeader("Content-Type", "application/json")
            .build();
            JsonNode jsonNode = this.executeRequest(request);
            return jsonNode.get("id").asText();
    }
    public String createAssistant(
        Models model, String name, List<AssistantTools> tools, String instructions
    ) throws IOException {
        return this.createAssistant(model, name, tools, instructions, new ArrayList<>(), new ArrayList<>());
    }
    public String createAssistant(
        Models model, String name, List<AssistantTools> tools, String instructions, List<String> fileIds, List<String> fsVectorIds
    ) throws IOException {

        String json = objectMapper.writeValueAsString(new CreateAssistantRequest(
            model, name, tools, instructions, fileIds, fsVectorIds
        ));
        //System.out.println(json);
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("OpenAI-Beta", "assistants=v2")
            .addHeader("Content-Type", "application/json")
            .build();
            JsonNode jsonNode = this.executeRequest(request);
            return jsonNode.get("id").asText();

    }
    // Warning: Remove all assistants created on OpenAI
    public void cleanUpAssistants() {
        try {
            List<String> assistantIds=this.listAssistants();
            System.out.println("there is "+assistantIds.size()+" assistants:");   
            assistantIds.forEach(assistantId -> {
                try {
                    this.deleteAssistant(assistantId);
                    System.out.println("Deleted assistant "+assistantId+ "...");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            System.out.println("Deleted all assistants");
            assistantIds=this.listAssistants();
            System.out.println("there is "+assistantIds.size()+" assistant left.");
        } catch (IOException e) {
            e.printStackTrace();    
        }
    }
    public List<String> listAssistants() throws IOException {

        Request request = new Request.Builder()
            .url(API_URL+"?order=desc&limit=20")
            .get()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();

        JsonNode jsonNode = this.executeRequest(request);
        List<String> assistantIds=new ArrayList<>();
        for (JsonNode assistant : jsonNode.get("data")) {
            assistantIds.add(assistant.get("id").asText());
        }
        return assistantIds;
    }
    public JsonNode retrieveAssistant(String assistantId) throws IOException {
        Request request = new Request.Builder()
            .url(API_URL+"/"+assistantId)
            .get()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();

            JsonNode jsonNode = this.executeRequest(request);
            return jsonNode;
    }
    public JsonNode modifyAsssistant( 
        ModifyAssistantRequest marRequest, String existingInstruction, String id
    ) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        String json = new ObjectMapper().writeValueAsString(
            marRequest.toOaiModifyAssistantRequest(existingInstruction)
        );
        RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
            .url(API_URL+"/"+id)
            .post(body)
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("OpenAI-Beta", "assistants=v2")
            .addHeader("Content-Type", "application/json")
            .build();
            System.out.println("Request:"+json);

        return this.executeRequest(request);
    }
    public void deleteAssistant(String assistantId) throws IOException {

        Request request = new Request.Builder()
            .url(API_URL+"/"+assistantId)
            .delete()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .build();

        this.executeRequest(request);
    }

    public static class CreateAssistantRequest {
        public final String name;
        public final List<Tool> tools;
        public final String instructions;
        public final String model;
        public final ToolResources tool_resources;
        public CreateAssistantRequest(Models model, String name, List<AssistantTools> tools, String instructions) {
            this.name = name;
            this.tools = new ArrayList<>();
            this.instructions = instructions;
            this.model = model.toString(); 
            this.tool_resources = new ToolResources(new ArrayList<>(), new ArrayList<>());
            for (AssistantTools tool : tools) {
                this.tools.add(new Tool(tool.name()));
            }
        }
        public CreateAssistantRequest(Models model, String name, List<AssistantTools> tools, String instructions, List<String> fileIds, List<String> fsVectorIds) {
            this.name = name;
            this.tools = new ArrayList<>();
            this.instructions = instructions;
            this.model = model.toString(); 
            this.tool_resources = new ToolResources(fileIds, fsVectorIds);
            for (AssistantTools tool : tools) {
                this.tools.add(new Tool(tool.name()));
            }
        }
        
    }
    record Tool (String type){}
    public static class ToolResources {
        public final CodeInterpreterResources code_interpreter;
        public final FileSearchResources file_search;
        public ToolResources(List<String> fileIds, List<String> fsVectorIds) {
            this.code_interpreter = new CodeInterpreterResources(fileIds);
            this.file_search = new FileSearchResources(fsVectorIds);
        }
    }
    public static class CodeInterpreterResources {
        public final List<String> file_ids;
        public CodeInterpreterResources(List<String> fileIds) {
            this.file_ids = fileIds;
        }
    }
    public static class FileSearchResources {
        public final List<String> vector_store_ids;
        public FileSearchResources(List<String> vectorIds) {
            this.vector_store_ids = vectorIds;
        }
    }
    public static AssistantBuilder testAssistantService() {
        return new AssistantBuilder(Models.gpt_4o)
            .setName("Code chat assistant")
            .setDescription("Help in the reviewing of code")
            .setInstructions("You are a code reviewer. When asked a question, provide feedback on the code base provided in your vector store.")
            .setReasoningEffort(ReasoningEfforts.high)
            .addFileSearchTool()
            .addFileSearchAssist()
            .setToolResourcesFileSearch(Set.of("vs_67aec52acf3c819198ef877500651d8f"))
            .addFunction()
                .setFunctionName("countLines")
                .setFunctionDescription("This function will return the number of lines in a file")
                .FunctionAddParameter("fileid", "string", "The id of the file")
            .addFunction()
                .setFunctionName("getFilename")
                .setFunctionDescription("This function will return the name of file")
                .FunctionAddParameter("fileid", "string", "The id of the file");
                
    }


}
/*
 * Curls samples of Assistant api
 * Create
 curl "https://api.openai.com/v1/assistants" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "OpenAI-Beta: assistants=v2" \
  -d '{
    "instructions": "You are a personal math tutor. When asked a question, write and run Python code to answer the question.",
    "name": "Math Tutor",
    "tools": [{"type": "code_interpreter"}],
    "model": "gpt-4o"
  }'
  * List
  curl "https://api.openai.com/v1/assistants?order=desc&limit=20" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "OpenAI-Beta: assistants=v2"
  * Retrieve
  curl https://api.openai.com/v1/assistants/asst_abc123 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "OpenAI-Beta: assistants=v2"
  * Modify
  curl https://api.openai.com/v1/assistants/asst_abc123 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "OpenAI-Beta: assistants=v2" \
  -d '{
      "instructions": "You are an HR bot, and you have access to files to answer employee questions about company policies. Always response with info from either of the files.",
      "tools": [{"type": "file_search"}],
      "model": "gpt-4o"
    }'
    * delete
    https://api.openai.com/v1/assistants/{assistant_id}

 */
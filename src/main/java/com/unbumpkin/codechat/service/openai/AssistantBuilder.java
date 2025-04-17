package com.unbumpkin.codechat.service.openai;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;

@JsonSerialize
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssistantBuilder {
    public enum AssistantTools {
        code_interpreter,
        file_search,
        function
    }
    public enum ReasoningEfforts {
        low,
        medium,
        high
    }
    @JsonProperty("model")
    final String model;
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @JsonProperty("instructions")
    String instructions;
    ReasoningEfforts reasoningEffort;
    @JsonProperty("tools")
    final Set<Tool> tools;
    @JsonProperty("tool_resources")
    Map<String, ToolResources> tool_resources;
    @JsonProperty("metadata")
    Map<String, String> metadata;
    @JsonProperty("temperature")
    Double temperature;
    @JsonProperty("top_p")
    Double top_p;
    @JsonProperty("response_format")
    Object response_format;
    @JsonIgnore
    private int functionIndex;

    public AssistantBuilder(
        Models model
    ) {
        this.model = model.toString();
        this.tools = new HashSet<>();
        this.top_p = null;
        this.response_format = null;
        this.functionIndex = -1;
    }
    public AssistantBuilder setName(String name) {
        this.name = name;
        return this;
    }
    public AssistantBuilder setDescription(String description) {
        this.description = description;
        return this;
    }
    public AssistantBuilder setInstructions(String instructions) {
        this.instructions = instructions;
        return this;
    }
    public AssistantBuilder setMetadata(Map<String, String> metadata) {
        this.metadata=metadata;
        return this;
    }
    public AssistantBuilder setReasoningEffort(ReasoningEfforts reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
        return this;
    }
    public AssistantBuilder setTemperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }
    public AssistantBuilder setTopP(Double top_p) {
        this.top_p = top_p;
        return this;
    }
    public AssistantBuilder addCodeInterpreterAssist() {
        tools.add(new CodeInterpreterTool());
        return this;
    }
    public AssistantBuilder addFileSearchTool() {
        tools.add(new FileSearchTool());
        return this;
    }
    public AssistantBuilder addFileSearchAssist() {
        boolean hasFileSearchTool = tools.stream()
            .anyMatch(tool -> tool.type == AssistantTools.file_search);
        if (!hasFileSearchTool) {
            throw new IllegalArgumentException("File search tool must be added before adding file search");
        }
        FileSearchTool fs =this.tools.stream()
            .filter(tool -> tool.type == AssistantTools.file_search)
            .map(tool -> (FileSearchTool) tool)
            .findFirst()
            .get();
        fs.file_search = new FileSearch();
        return this;
    }
    public AssistantBuilder setFileSearchMaxNumResults(Integer maxNumResults) {
        boolean hasFileSearchTool = tools.stream()
            .anyMatch(tool -> tool.type == AssistantTools.file_search);
        if (!hasFileSearchTool) {
            throw new IllegalArgumentException("File search tool must be added before setting max num results");
        }
        FileSearchTool fs =this.tools.stream()
            .filter(tool -> tool.type == AssistantTools.file_search)
            .map(tool -> (FileSearchTool) tool)
            .findFirst()
            .get();
        fs.file_search.max_num_results = maxNumResults;
        return this;
    }
    public AssistantBuilder setFileSearchRankingOption(Double scoreThreshold) {
        return this.setFileSearchRankingOption("auto", scoreThreshold);
    }
    public AssistantBuilder setFileSearchRankingOption(String ranker, Double scoreThreshold) {
            boolean hasFileSearchTool = tools.stream()
            .anyMatch(tool -> tool.type == AssistantTools.file_search);
        if (!hasFileSearchTool) {
            throw new IllegalArgumentException("File search tool must be added before setting ranking options");
        }
        FileSearchTool fs =this.tools.stream()
            .filter(tool -> tool.type == AssistantTools.file_search)
            .map(tool -> (FileSearchTool) tool)
            .findFirst()
            .get();
        fs.file_search.ranking_options = new RankingOptions();
        fs.file_search.ranking_options.ranker = ranker;
        fs.file_search.ranking_options.score_threshold = scoreThreshold;
        return this;
    }
    public AssistantBuilder addFunction() {
        this.functionIndex++;
        tools.add(new FunctionTool());
        return this;
    }
    public AssistantBuilder setFunctionName(String name) {
        boolean hasFunctionTool = tools.stream()
            .anyMatch(tool -> tool.type == AssistantTools.function);
        if (!hasFunctionTool) {
            throw new IllegalArgumentException("Function tool must be added before setting function");
        }
        FunctionTool functionTool = this.tools.stream()
            .filter(tool -> tool.type == AssistantTools.function)
            .map(tool -> (FunctionTool) tool)
            .skip(this.functionIndex)
            .findFirst()
            .get();
        functionTool.function = new Function(name);
        return this;
    }
    public AssistantBuilder setFunctionDescription(String description) {
        boolean hasFunctionTool = tools.stream()
            .anyMatch(tool -> tool.type == AssistantTools.function);
        if (!hasFunctionTool) {
            throw new IllegalArgumentException("Function tool must be added before setting function description");
        }
        FunctionTool functionTool = this.tools.stream()
            .filter(tool -> tool.type == AssistantTools.function)
            .map(tool -> (FunctionTool) tool)
            .skip(this.functionIndex)
            .findFirst()
            .get();
        functionTool.function.description = description;
        return this;
    }
    public AssistantBuilder FunctionAddParameter(String name, String type, String description) {
        boolean hasFunctionTool = tools.stream()
            .anyMatch(tool -> tool.type == AssistantTools.function);
        if (!hasFunctionTool) {
            throw new IllegalArgumentException("Function tool must be added before setting function parameters");
        }
        FunctionTool functionTool = this.tools.stream()
            .filter(tool -> tool.type == AssistantTools.function)
            .map(tool -> (FunctionTool) tool)
            .skip(this.functionIndex)
            .findFirst()
            .get();
        functionTool.function.parameters.properties.put(name, new TypeDescription(type, description));
        return this;
    }
    public AssistantBuilder setFunctionStrict(Boolean strict) {
        boolean hasFunctionTool = tools.stream()
            .anyMatch(tool -> tool.type == AssistantTools.function);
        if (!hasFunctionTool) {
            throw new IllegalArgumentException("Function tool must be added before setting function strict");
        }
        FunctionTool functionTool = this.tools.stream()
            .filter(tool -> tool.type == AssistantTools.function)
            .map(tool -> (FunctionTool) tool)
            .skip(this.functionIndex)
            .findFirst()
            .get();
        functionTool.function.strict = strict;
        return this;
    }
    public AssistantBuilder setToolResourcesCodeInterpreter(Set<String> fileIds) {
        boolean hasCodeInterpreterTool = tools.stream()
            .anyMatch(tool -> tool.type == AssistantTools.code_interpreter);
        if (!hasCodeInterpreterTool) {
            throw new IllegalArgumentException("Code interpreter tool must be added before setting tool resources");
        }
        if (tool_resources == null) {
            tool_resources = new HashMap<>();
        }
        ToolResources codeInterpreterResource = new ToolResources();
        codeInterpreterResource.file_ids = fileIds;
        tool_resources.put("code_interpreter", codeInterpreterResource);
        return this;
    }
    public AssistantBuilder setToolResourcesFileSearch(Set<String> vectorStoreIds) {
        boolean hasFileSearchTool = tools.stream()
            .anyMatch(tool -> tool.type == AssistantTools.file_search);
        if (!hasFileSearchTool) {
            throw new IllegalArgumentException("File search tool must be added before setting tool resources");
        }
        if (tool_resources == null) {
            tool_resources = new HashMap<>();
        }
        ToolResources fileSearchResource = new ToolResources();
        fileSearchResource.vector_store_ids = vectorStoreIds;
        tool_resources.put("file_search", fileSearchResource);
        return this;
    }
    public AssistantBuilder addAutoResponseFormat() {
        this.response_format = "auto";
        return this;
    }
    public AssistantBuilder addOutsideJsonSchemaResponseFormat() {
        this.response_format = "{json_schema}";
        return this;
    }
    public AssistantBuilder addTextResponseFormat() {
        this.response_format = new TextResponseFormat();
        return this;
    }
    public AssistantBuilder addJSonObjectResponseFormat() {
        this.response_format = new JSonObjectResponseFormat();
        return this;
    }
    public AssistantBuilder addJSonSchemaResponseFormat(String name) {
        this.response_format = new JSonSchemaResponseFormat();
        ((JSonSchemaResponseFormat) this.response_format).json_schema = new JSonSchema(name);
        return this;
    }
    public AssistantBuilder setJSonSchemaDescription(String description) {
        boolean hasJSonSchemaResponseFormat = this.response_format instanceof JSonSchemaResponseFormat;
        if (!hasJSonSchemaResponseFormat) {
            throw new IllegalArgumentException("JSon schema response format must be added before setting description");
        }
        ((JSonSchemaResponseFormat) this.response_format).json_schema.description = description;
        return this;
    }
    public AssistantBuilder setJSonSchemaStrict(Boolean strict) {
        boolean hasJSonSchemaResponseFormat = this.response_format instanceof JSonSchemaResponseFormat;
        if (!hasJSonSchemaResponseFormat) {
            throw new IllegalArgumentException("JSon schema response format must be added before setting strict");
        }
        ((JSonSchemaResponseFormat) this.response_format).json_schema.strict = strict;
        return this;
    }
    public AssistantBuilder setJSonSchemaSchema(String schema) {
        boolean hasJSonSchemaResponseFormat = this.response_format instanceof JSonSchemaResponseFormat;
        if (!hasJSonSchemaResponseFormat) {
            throw new IllegalArgumentException("JSon schema response format must be added before setting schema");
        }
        ((JSonSchemaResponseFormat) this.response_format).json_schema.schema = schema;
        return this;
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class Tool {
        @JsonProperty("type")
        public final AssistantTools type;
        public Tool(AssistantTools type) {
            this.type = type;
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class CodeInterpreterTool extends Tool {
        public CodeInterpreterTool() {
            super(AssistantTools.code_interpreter);
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class FileSearchTool extends Tool {
        @JsonProperty("file_search")
        FileSearch file_search;
        public FileSearchTool() {
            super(AssistantTools.file_search);
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class FunctionTool extends Tool {
        @JsonProperty("function")
        Function function;
        public FunctionTool() {
            super(AssistantTools.function);
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class FileSearch {
        @JsonProperty("max_num_results")
        public Integer max_num_results;
        @JsonProperty("ranking_options")
        public RankingOptions ranking_options;
        public FileSearch() {
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class RankingOptions {
        @JsonProperty("ranker")
        public String ranker;
        @JsonProperty("score_threshold")
        public Double score_threshold;
        public RankingOptions() {
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class Function {
        @JsonProperty("description")
        String description;
        @JsonProperty("name")
        String name;
        @JsonProperty("parameters")
        Parameters parameters;
        @JsonProperty("strict")
        Boolean strict;
        public Function(String name) {
            this.name = name;
            this.strict = false;
            this.parameters = new Parameters();
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class Parameters {
        @JsonProperty("type")
        String type;
        @JsonProperty("properties")
        Map<String, TypeDescription> properties;
        public Parameters() {
            type = "object";
            this.properties = new HashMap<>();
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class TypeDescription {
        @JsonProperty("description")
        String description;
        @JsonProperty("type")
        String type;
        public TypeDescription(String type, String description) {
            this.type = type;
            this.description = description;
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class ToolResources {
        @JsonProperty("file_ids")
        public Set<String> file_ids;
        @JsonProperty("vector_store_ids")
        public Set<String> vector_store_ids;
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class ResponseFormat {}
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class TextResponseFormat extends ResponseFormat {
        @JsonProperty("type")
        String type;
        public TextResponseFormat() {
            this.type="text";
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class JSonObjectResponseFormat extends ResponseFormat {
        @JsonProperty("type")
        String type;
        public JSonObjectResponseFormat() {
            this.type="json_object";
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class JSonSchemaResponseFormat extends ResponseFormat {
        @JsonProperty("type")
        String type;
        @JsonProperty("json_schema")
        JSonSchema json_schema;
        public JSonSchemaResponseFormat() {
            this.type="json_object";
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)    
    public static class JSonSchema {
        @JsonProperty("description")
        String description;
        @JsonProperty("name")
        String name;
        @JsonProperty("schema")
        String schema;
        @JsonProperty("strict")
        Boolean strict;
        public JSonSchema(String name) {
            this.name = name;
            this.strict = false;
        }
    }

}

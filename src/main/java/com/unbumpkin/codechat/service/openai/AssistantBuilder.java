package com.unbumpkin.codechat.service.openai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unbumpkin.codechat.service.openai.BaseOpenAIClient.Models;

public class AssistantBuilder {
    public enum AssistantTools {
        code_interpreter,
        file_search,
        function
    }
    public enum ReasoningEffort {
        low,
        medium,
        high
    }
    final Models model;
    String name;
    String description;
    String instructions;
    ReasoningEffort reasoningEffort;
    final Set<Tool> tools;
    ToolResources tool_resources;
    Map<String, String> metadata;
    Double temperature;
    Double top_p;
    Object response_format;
    @JsonIgnore
    private int functionIndex;

    public AssistantBuilder(
        Models model
    ) {
        this.model = model;
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
    public AssistantBuilder setReasoningEffort(ReasoningEffort reasoningEffort) {
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
    public AssistantBuilder setToolResourcesCodeInterpreter(List<String> fileIds) {
        boolean hasCodeInterpreterTool = tools.stream()
            .anyMatch(tool -> tool.type == AssistantTools.code_interpreter);
        if (!hasCodeInterpreterTool) {
            throw new IllegalArgumentException("Code interpreter tool must be added before setting tool resources");
        }
        CodeInterpreterTool codeInterpreterTool = this.tools.stream()
            .filter(tool -> tool.type == AssistantTools.code_interpreter)
            .map(tool -> (CodeInterpreterTool) tool)
            .findFirst()
            .get();
        codeInterpreterTool.tool_Ressources.file_ids = fileIds;
        return this;
    }
    public AssistantBuilder setToolResourcesFileSearch(List<String> vectorStoreIds) {
        boolean hasFileSearchTool = tools.stream()
            .anyMatch(tool -> tool.type == AssistantTools.file_search);
        if (!hasFileSearchTool) {
            throw new IllegalArgumentException("File search tool must be added before setting tool resources");
        }
        FileSearchTool fileSearchTool = this.tools.stream()
            .filter(tool -> tool.type == AssistantTools.file_search)
            .map(tool -> (FileSearchTool) tool)
            .findFirst()
            .get();
        fileSearchTool.tool_Ressources.vector_store_ids = vectorStoreIds;
        return this;
    }
    public AssistantBuilder addAutoResponseFormat() {
        this.response_format = "auto";
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
    public static class Tool {
        public final AssistantTools type;
        public Tool(AssistantTools type) {
            this.type = type;
        }
    }
    public static class CodeInterpreterTool extends Tool {
        CodeInterpreterToolRessources tool_Ressources;
        public CodeInterpreterTool() {
            super(AssistantTools.code_interpreter);
            this.tool_Ressources = new CodeInterpreterToolRessources();
        }
    }
    public static class FileSearchTool extends Tool {
        FileSearch file_search;
        FileSearchToolRessources tool_Ressources;
        public FileSearchTool() {
            super(AssistantTools.file_search);
            this.tool_Ressources = new FileSearchToolRessources();
        }
    }
    public static class FunctionTool extends Tool {
        Function function;
        public FunctionTool() {
            super(AssistantTools.function);
        }
    }

    public static class FileSearch {
        public Integer max_num_results;
        public RankingOptions ranking_options;
        public FileSearch() {
        }
    }
    public static class RankingOptions {
        public String ranker;
        public Double score_threshold;
        public RankingOptions() {
        }
    }

    public static class Function {
        String description;
        String name;
        Parameters parameters;
        Boolean strict;
        public Function(String name) {
            this.name = name;
            this.strict = false;
            this.parameters = new Parameters();
        }
    }
    public static class Parameters {
        String type;
        Map<String, TypeDescription> properties;
        public Parameters() {
            type = "object";
            this.properties = new HashMap<>();
        }
    }
    public static class TypeDescription {
        String description;
        String type;
        public TypeDescription(String type, String description) {
            this.type = type;
            this.description = description;
        }
    }
    public static class ToolResources {
    }
    public static class CodeInterpreterToolRessources extends ToolResources {
        public List<String> file_ids;
        public CodeInterpreterToolRessources() {
            this.file_ids = new ArrayList<>();
        }
    }
    public static class FileSearchToolRessources extends ToolResources {
        public List<String> vector_store_ids;
        public FileSearchToolRessources() {
            this.vector_store_ids = new ArrayList<>();
        }
    }
    public static class ResponseFormat {}
    public static class TextResponseFormat extends ResponseFormat {
        String type;
        public TextResponseFormat() {
            this.type="text";
        }
    }
    public static class JSonObjectResponseFormat extends ResponseFormat {
        String type;
        public JSonObjectResponseFormat() {
            this.type="json_object";
        }
    }
    public static class JSonSchemaResponseFormat extends ResponseFormat {
        String type;
        JSonSchema json_schema;
        public JSonSchemaResponseFormat() {
            this.type="json_object";
        }
    }
    public static class JSonSchema {
        String description;
        String name;
        String schema;
        Boolean strict;
        public JSonSchema(String name) {
            this.name = name;
            this.strict = false;
        }
    }

}

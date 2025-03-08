package com.unbumpkin.codechat.service.openai;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.unbumpkin.codechat.model.openai.OaiFile;
import com.unbumpkin.codechat.model.openai.OaiFile.Purposes;
import com.unbumpkin.codechat.util.FileUtils;

import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class OaiFileService  extends BaseOpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/files";
    private static final String API_URL_WITH_FILE = "https://api.openai.com/v1/files/%s";
    private static final String API_URL_WITH_FILE_CONTENT = "https://api.openai.com/v1/files/%s/content";

    public OaiFileService() {
        super();
    }
    /**
     * Upload recursively all files in a directory with a specific extension, specifically for Assistants 
     * @param rootDir: the root directory to start the search
     * @param extension: the extension of the files to upload
     * @return a map of the file id and the OaiFile object
     * @throws IOException
     */
    public Map<String,OaiFile> uploadFiles(
        String rootDir, String extension, int projectId
    ) throws IOException {
        return this.uploadFiles(rootDir, extension, Purposes.assistants, projectId);
    }
    /**
     * Upload recursively all files in a directory with a specific extension 
     * @param rootDir: the root directory to start the search
     * @param extension: the extension of the files to upload
     * @param purpose: the purpose of the file upload
     * @return a map of the file id and the OaiFile object
     * @throws IOException
     */
    public Map<String,OaiFile> uploadFiles(
        String rootDir, String extension, Purposes purpose, int projectId
    ) throws IOException {
        //recursivly read a directory and find files with a specific extension
        List<File> files= FileUtils.listFiles(rootDir, Set.of(extension));

        Map<String,OaiFile> fileIdMap=new HashMap<>(files.size());
        System.out.println("Uploading files...");
        for(File file:files) {
            if(file.length()==0) {
                System.out.println("File "+file.getName()+" is empty, skipping...");
                continue;
            }
            OaiFile oaiFile=this.uploadFile(file.getAbsolutePath(), purpose, projectId);
            fileIdMap.put(oaiFile.fileId(), oaiFile);
            System.out.println(String.format("File %s uploaded with id: %s...",file,oaiFile.fileId()));
        }
        return fileIdMap; 
    }
    /**
     * Remove all files uploaded to OpenAI
     * Warning this will delete all files uploaded to OpenAI
     * @return a list of the deleted file ids
     * @throws IOException
     */
    public List<String> cleanUpFiles() throws IOException {
        List<String> fileIds=this.listFiles();
        List<String> deletedFileIds=new ArrayList<>(fileIds.size());
        System.out.println("there is "+fileIds.size()+" files:");   
        System.out.println("Deleting all files...");
        fileIds.forEach(fileId_ -> {
            try {
                this.deleteFile(fileId_);
                deletedFileIds.add(fileId_);
                System.out.println("Deleted file "+fileId_+ "...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        fileIds=this.listFiles();
        System.out.println("there is "+fileIds.size()+" file left.");
        return deletedFileIds;
    }

    /**
     * Upload a file to OpenAI
     * @param filePath: the path of the file to upload
     * @param purpose: the purpose of the file upload
     * @return the OaiFile object
     * @throws IOException
     */
    public OaiFile uploadFile(String filePath, Purposes purpose, int projectId) throws IOException {
        String url = API_URL;

        File file=new java.io.File(filePath);
        int linecount=FileUtils.countLines(file);
        RequestBody fileBody = RequestBody.create(file, JSON_MEDIA_TYPE);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("purpose", purpose.toString())
                .addFormDataPart("file", filePath, fileBody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();
        OaiFile oaiFile = new OaiFile(0,
            projectId,
            this.executeRequest(request).get("id").asText(),
            Paths.get(filePath).getFileName().toString(),
            Paths.get(filePath).getParent().toString(),
            filePath,
            purpose,
            linecount
        );
        return oaiFile;
    }

    /**
     * List all files in OpenAI storage
     * @return a list of the file ids
     * @throws IOException
     */
    public List<String> listFiles() throws IOException {
        String url = API_URL;

        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .build();

        JsonNode jsonNode = this.executeRequest(request);
        List<String> fileIds = new ArrayList<>();
        for (JsonNode file : jsonNode.get("data")) {
            fileIds.add(file.get("id").asText());
        }
        return fileIds;
    }

    /**
     * Retrieve a file from OpenAI
     * @param fileId: the id of the file to retrieve
     * @return the file as a JsonNode
     * @throws IOException
     */
    public JsonNode retrieveFile(String fileId) throws IOException {
        String url = String.format(API_URL_WITH_FILE, fileId);

        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .build();

        return this.executeRequest(request);
    }

    /**
     * Delete a file from OpenAI
     * @param fileId: the id of the file to delete
     * @throws IOException
     */
    public void deleteFile(String fileId) throws IOException {
        String url = String.format(API_URL_WITH_FILE, fileId);

        Request request = new Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .build();

        this.executeRequest(request);
    }

    /**
     * Download the content of a file from OpenAI
     * @param fileId: the id of the file to download
     * @param outputPath: the path to save the file
     * @throws IOException
     */
    public void downloadFile(
        @RequestParam String fileId, String outputPath) throws IOException {
        String url = String.format(API_URL_WITH_FILE_CONTENT, fileId);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body().string();
                throw new IOException(responseBody);
            }
            Files.write(Paths.get(outputPath), response.body().bytes());
            System.out.println("File content saved to " + outputPath);
        }
    }

    /**
     * Download the content of a file from OpenAI
     * @param fileId: the id of the file to download
     * @return the content of the file as a byte array
     * @throws IOException
     */
    public byte[] downloadFile(String fileId) throws IOException {
        String url = String.format(API_URL_WITH_FILE_CONTENT, fileId);

        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer " + API_KEY)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body().string();
                throw new IOException(responseBody);
            }
            return response.body().bytes();
        }
    }

}
/*
 * Curls samples of File api
  * Upload
    curl https://api.openai.com/v1/files \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -F purpose="assistants" \
    -F file="/Users/benoitmarsot/dev/python/grip-no-tests/grip/vendor/__init__.py"
  * List
    curl https://api.openai.com/v1/files \
    -H "Authorization: Bearer $OPENAI_API_KEY"
  * Retrieve
    curl https://api.openai.com/v1/files/file-abc123 \
    -H "Authorization: Bearer $OPENAI_API_KEY"
  * Delete
    curl https://api.openai.com/v1/files/file-abc123 \
    -X DELETE \
    -H "Authorization: Bearer $OPENAI_API_KEY"
  * GetContent
    curl https://api.openai.com/v1/files/file-8vkU2nQC8NtBiV5TX1gbN6/content \
    -H "Authorization: Bearer $OPENAI_API_KEY" > graph3.png

*/
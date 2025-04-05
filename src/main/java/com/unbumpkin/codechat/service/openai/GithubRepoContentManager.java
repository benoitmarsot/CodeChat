package com.unbumpkin.codechat.service.openai;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.dto.GitHubChangeTracker;
import com.unbumpkin.codechat.dto.GitHubChangeTracker.ChangeTypes;
import com.unbumpkin.codechat.dto.GitHubChangeTracker.GitHubChange;

// This class extends the ProjectFileCategorizer 
// and adds functionality to clone a GitHub repository and categorize its files.
public class GithubRepoContentManager extends CCProjectFileManager {

    private final String username;
    private final String password;
    private File tempDir;
    private String commitHash;

    public GithubRepoContentManager(String username, String password) {
        super();
        this.tempDir = null;
        this.username = username;
        this.password = password;
        this.commitHash = null;
    }
    public GithubRepoContentManager() {
        super();
        this.tempDir = null;
        this.username = null;
        this.password = null;
    }
    public String getCommitHash() {
        return commitHash;
    }

    /**
     * Clones the given GitHub repository and categorizes its files using the inherited addDir method.
     * @param repoUrl the URL of the GitHub repository (e.g., "https://github.com/user/repo.git")
     * @param branch  the branch to clone (e.g., "main" or "master")
     * 
     * @throws GitAPIException if an error occurs during cloning
     * @throws IOException if an I/O error occurs
     * @throws RateLimitExceededException if the GitHub API rate limit is exceeded
     * 
     * @return the path to the cloned repository
     */
    public String addRepository(
        String repoUrl, String branch
    ) throws GitAPIException, IOException, RateLimitExceededException {
        checkRateLimit(true); // Check rate limit before cloning
        // Create a temporary directory to clone the repository
        tempDir = Files.createTempDirectory("github-repo-").toFile();
        // Clone the repository using JGit
        CloneCommand cloneCmd=Git.cloneRepository()
            .setURI(repoUrl)
            .setBranch(branch)
            .setDirectory(tempDir)
            .setCloneAllBranches(false) //only fetch the selected branch
            .setDepth(1) ; //shallow clone, only the latest commit
        // Set credentials if provided
        if(username != null && !username.isEmpty()) {
            cloneCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                username, password==null?"":password
            ));
        }
        cloneCmd.call();
        try(Git git=Git.open(tempDir)) {
            // Get the latest commit
            Iterable<RevCommit> logs = git.log().setMaxCount(1).call();
            RevCommit latestCommit = logs.iterator().next();
            this.commitHash = latestCommit.getName(); // Returns the full hash

            // Use the inherited method to scan and categorize files in the cloned repository
            super.addDir(tempDir.getAbsolutePath());
        }
        return tempDir.getAbsolutePath();
    }

    public void deleteRepository() {
        if (tempDir != null && tempDir.exists()) {
            deleteDirectoryRecursively(tempDir);
            tempDir = null; // Reset tempDir to avoid reuse
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
    /**
     * Gets the hash of the latest commit without cloning the repository.
     * This method is much faster as it only queries the remote references.
     * 
     * @param repoUrl the URL of the GitHub repository
     * @param branch the branch to check (e.g., "main" or "master")
     * @return the commit hash as a string
     * @throws GitAPIException if an error occurs during Git operations
     */
    public String getLatestCommitHash(String repoUrl, String branch) throws GitAPIException {
        LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository()
            .setRemote(repoUrl)
            .setHeads(true);
        
        // Set credentials if provided
        if (username != null && !username.isEmpty()) {
            lsRemoteCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                username, password == null ? "" : password
            ));
        }
        
        Collection<Ref> refs = lsRemoteCommand.call();
        
        // Look for the specified branch
        String refName = "refs/heads/" + branch;
        for (Ref ref : refs) {
            if (ref.getName().equals(refName)) {
                return ref.getObjectId().getName();
            }
        }
        
        throw new IllegalArgumentException("Branch not found: " + branch);
    }

    /**
     * Gets changes between commits using GitHub's API without cloning the repository.
     * 
     * @param repoUrl the URL of the GitHub repository
     * @param sinceCommit the commit hash to compare against
     * @param branch the branch to compare
     * @return a GitHubChangeTracker containing change information and the output directory
     * @throws IOException if an error occurs during the API request
     */
    public GitHubChangeTracker getChangesSinceCommitViaGitHubAPI(
        String repoUrl, String sinceCommit, String branch
    ) throws IOException {
        // Extract owner and repo name from URL
        String repoPath = repoUrl.replace("https://github.com/", "")
                                .replace("http://github.com/", "")
                                .replace(".git", "");
        
        // Build API URL for comparison
        String compareUrl = "https://api.github.com/repos/" + repoPath + "/compare/" + sinceCommit + "..." + branch;
        
        // Create a directory for the modified files
        File outputDir = Files.createTempDirectory("changed-files-").toFile();
        
        // Lists to track file changes
        List<GitHubChange> changes = new ArrayList<>();
        List<String> addedFiles = new ArrayList<>();
        List<String> deletedFiles = new ArrayList<>();
        
        try {
            // Make API request
            URL url = new URL(compareUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            
            // Add authentication if available
            if (username != null && !username.isEmpty()) {
                String auth = username + ":" + (password == null ? "" : password);
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
            }
            
            // Read the response
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("GitHub API request failed with status code: " + responseCode);
            }
            
            // Parse JSON response using Jackson
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(connection.getInputStream());
            JsonNode files = json.get("files");
            
            // Process each file
            for (JsonNode fileJson : files) {
                String status = fileJson.get("status").asText();
                String filePath = fileJson.get("filename").asText();
                
                switch (status) {
                    case "added":
                        // Add to the changes list with ADDED type
                        changes.add(new GitHubChange(filePath, ChangeTypes.ADDED));
                        addedFiles.add(filePath);
                        
                        // Download and save the file
                        downloadFileContent(fileJson, outputDir, filePath);
                        break;
                        
                    case "modified":
                        // The GitHubChangeTracker has a MODIFIED type, so use it directly
                        changes.add(new GitHubChange(filePath, ChangeTypes.MODIFIED));
                        // For tracking purposes, we also consider this an add and delete (since we're downloading it)
                        addedFiles.add(filePath);
                        deletedFiles.add(filePath);

                        // Download and save the file
                        downloadFileContent(fileJson, outputDir, filePath);
                        break;
                        
                    case "renamed":
                        // Renamed file - mark old path as deleted, new path as added
                        String previousPath = fileJson.get("previous_filename").asText();
                        
                        changes.add(new GitHubChange(previousPath, ChangeTypes.DELETED));
                        changes.add(new GitHubChange(filePath, ChangeTypes.ADDED));
                        
                        deletedFiles.add(previousPath);
                        addedFiles.add(filePath);
                        
                        // Download and save the file
                        downloadFileContent(fileJson, outputDir, filePath);
                        break;
                        
                    case "removed":
                        // Removed file - mark as deleted
                        changes.add(new GitHubChange(filePath, ChangeTypes.DELETED));
                        deletedFiles.add(filePath);
                        break;
                        
                    case "copied":
                        // Copied file - mark as added
                        changes.add(new GitHubChange(filePath, ChangeTypes.ADDED));
                        addedFiles.add(filePath);
                        
                        // Download and save the file
                        downloadFileContent(fileJson, outputDir, filePath);
                        break;
                }
            }
            
            // Use the inherited method to scan and categorize files in the output directory
            super.addDir(outputDir.getAbsolutePath());
            
            // Update the temp directory
            tempDir = outputDir;
            
            // Create and return the GitHubChangeTracker
            return new GitHubChangeTracker(
                outputDir.getAbsolutePath(),
                changes,
                addedFiles,
                deletedFiles,
                changes.size()
            );
            
        } catch (Exception e) {
            if (outputDir != null && outputDir.exists()) {
                deleteDirectoryRecursively(outputDir);
            }
            throw new IOException("Error processing GitHub API response: " + e.getMessage(), e);
        }
    }
    @Override
    public String getTempDir() {
        return tempDir.getAbsolutePath();
    }


    /**
     * Downloads file content from GitHub's API response and saves it to the output directory.
     */
    private void downloadFileContent(JsonNode fileJson, File outputDir, String filePath) throws IOException {
        // Check if we need to make a separate request for content
        if (!fileJson.has("content") || !fileJson.has("patch")) {
            // Make an additional API request to get the file content
            String fileUrl = fileJson.get("raw_url").asText();
            
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            // Add authentication if available
            if (username != null && !username.isEmpty()) {
                String auth = username + ":" + (password == null ? "" : password);
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
            }
            
            // Create the directory structure
            File destFile = new File(outputDir, filePath);
            destFile.getParentFile().mkdirs();
            
            // Download and save the file
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                FileWriter writer = new FileWriter(destFile)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.write(System.lineSeparator());
                }
            }
        } else {
            // For text files with content directly in the response
            String content = fileJson.get("content").asText();
            byte[] contentBytes = Base64.getDecoder().decode(content);
            
            // Create the directory structure
            File destFile = new File(outputDir, filePath);
            destFile.getParentFile().mkdirs();
            
            // Write the content to file
            Files.write(destFile.toPath(), contentBytes);
        }
    }
/**
 * Checks the current GitHub API rate limit status.
 * 
 * @param throwOnLow if true, throws an exception when rate limit is critically low
 * @return a map containing rate limit information
 * @throws IOException if an error occurs during the API request
 * @throws RateLimitExceededException if the rate limit is critically low and throwOnLow is true
 */
public GitHubRateLimit checkRateLimit(boolean throwOnLow) throws IOException, RateLimitExceededException {
    URL url = new URL("https://api.github.com/rate_limit");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
    
    // Add authentication if available (strongly recommended to get higher rate limits)
    if (username != null && !username.isEmpty()) {
        String auth = username + ":" + (password == null ? "" : password);
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
    }
    
    int responseCode = connection.getResponseCode();
    if (responseCode != 200) {
        throw new IOException("Failed to check GitHub rate limit: HTTP " + responseCode);
    }
    
    // Parse response
    ObjectMapper mapper = new ObjectMapper();
    JsonNode json = mapper.readTree(connection.getInputStream());
    JsonNode coreLimit = json.get("resources").get("core");
    
    int limit = coreLimit.get("limit").asInt();
    int remaining = coreLimit.get("remaining").asInt();
    long resetTimestamp = coreLimit.get("reset").asLong();
    
    // Calculate time until reset
    long currentTime = System.currentTimeMillis() / 1000;
    long waitSeconds = Math.max(0, resetTimestamp - currentTime);
    
    // Format reset time
    Instant resetInstant = Instant.ofEpochSecond(resetTimestamp);
    LocalDateTime resetTime = LocalDateTime.ofInstant(resetInstant, ZoneId.systemDefault());
    String formattedResetTime = resetTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    
    GitHubRateLimit rateLimit = new GitHubRateLimit(limit, remaining, resetTimestamp, waitSeconds, formattedResetTime);
    
    // Check if we're running too close to the limit
    if (throwOnLow && remaining < 10) {
        throw new RateLimitExceededException("GitHub API rate limit nearly exhausted: " + 
            remaining + " requests remaining. Resets at " + formattedResetTime + 
            " (" + formatDuration(waitSeconds) + ").", rateLimit);
    }
    
    return rateLimit;
}

/**
 * Formats duration in seconds to a human-readable string.
 */
    private String formatDuration(long seconds) {
        Duration duration = Duration.ofSeconds(seconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long remainingSeconds = duration.toSecondsPart();
        
        StringBuilder formatted = new StringBuilder();
        if (hours > 0) {
            formatted.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            formatted.append(minutes).append("m ");
        }
        formatted.append(remainingSeconds).append("s");
        
        return formatted.toString();
    }

    /**
     * Record class to hold GitHub rate limit information.
     */
    public record GitHubRateLimit(
        int limit,
        int remaining,
        long resetTimestamp,
        long secondsUntilReset,
        String formattedResetTime
    ) {
        /**
         * Checks if the rate limit is critically low (less than 10 requests remaining).
         */
        public boolean isCriticallyLow() {
            return remaining < 10;
        }
        
        /**
         * Waits until the rate limit resets if it's below the specified threshold.
         * 
         * @param minimumRemaining the minimum number of remaining requests before waiting
         * @throws InterruptedException if the wait is interrupted
         */
        public void waitIfNeeded(int minimumRemaining) throws InterruptedException {
            if (remaining < minimumRemaining) {
                System.out.println("GitHub API rate limit low: " + remaining + "/" + limit + 
                    " requests remaining. Waiting until reset at " + formattedResetTime + 
                    " (" + secondsUntilReset + " seconds)");
                
                Thread.sleep(secondsUntilReset * 1000);
            }
        }
    }

    /**
     * Exception thrown when the GitHub API rate limit is exceeded or critically low.
     */
    public static class RateLimitExceededException extends Exception {
        private final GitHubRateLimit rateLimit;
        
        public RateLimitExceededException(String message, GitHubRateLimit rateLimit) {
            super(message);
            this.rateLimit = rateLimit;
        }
        
        public GitHubRateLimit getRateLimit() {
            return rateLimit;
        }
    }
}


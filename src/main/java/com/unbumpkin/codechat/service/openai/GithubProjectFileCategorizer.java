package com.unbumpkin.codechat.service.openai;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

// This class extends the ProjectFileCategorizer 
// and adds functionality to clone a GitHub repository and categorize its files.
public class GithubProjectFileCategorizer extends CCProjectFileCategorizer {

    private final String username;
    private final String password;
    private File tempDir;
    public GithubProjectFileCategorizer(String username, String password) {
        super();
        this.tempDir = null;
        this.username = username;
        this.password = password;
    }
    public GithubProjectFileCategorizer() {
        super();
        this.tempDir = null;
        this.username = null;
        this.password = null;
    }

    /**
     * Clones the given GitHub repository and categorizes its files using the inherited addDir method.
     * 
     * @param repoUrl the URL of the GitHub repository (e.g., "https://github.com/user/repo.git")
     * @param branch  the branch to clone (e.g., "main" or "master")
     * @throws GitAPIException if an error occurs during cloning
     * @throws IOException if an I/O error occurs
     */
    public String addRepository(String repoUrl, String branch) throws GitAPIException, IOException {
        // Create a temporary directory to clone the repository
        tempDir = Files.createTempDirectory("github-repo-").toFile();
        // Clone the repository using JGit
        CloneCommand cloneCmd=Git.cloneRepository()
            .setURI(repoUrl)
            .setBranch(branch)
            .setDirectory(tempDir);

            // Todo: Set credential from UI
            if(username != null && !username.isEmpty()) {
                cloneCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                    username, password==null?"":password
                ));
            }
            cloneCmd.call();

        // Use the inherited method to scan and categorize files in the cloned repository
        super.addDir(tempDir.getAbsolutePath());
        return tempDir.getAbsolutePath();
    }

    /**
     * Overloaded method that clones the repository using a default branch.
     * You can modify the default branch as needed (e.g., "main" or "master").
     * 
     * @param repoUrl the URL of the GitHub repository
     * @throws GitAPIException if an error occurs during cloning
     * @throws IOException if an I/O error occurs
     */
    public void addRepository(String repoUrl) throws GitAPIException, IOException {
        // Modify the default branch if necessary
        String defaultBranch = "master"; // or "main"
        addRepository(repoUrl, defaultBranch);
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
}


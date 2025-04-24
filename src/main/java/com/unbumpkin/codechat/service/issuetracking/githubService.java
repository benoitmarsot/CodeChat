package com.unbumpkin.codechat.service.issuetracking;

import com.unbumpkin.codechat.dto.issuetracker.Issue;
import com.unbumpkin.codechat.dto.issuetracker.IssueComment;
import com.unbumpkin.codechat.dto.social.SocialUser;
import com.unbumpkin.codechat.model.ProjectResource.ResTypes;

import org.kohsuke.github.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class githubService extends IssueTrackingService {
    private final GitHub github;
    private final String repositoryName;

    /**
     * Constructor with Personal Access Token authentication
     * 
     * @param repositoryName The repository name in format "owner/repo"
     * @param pat Personal Access Token for GitHub authentication
     */
    public githubService(String repositoryName, String pat) {
        try {
            this.github = new GitHubBuilder().withOAuthToken(pat).build();
            this.repositoryName = repositoryName;
            // Verify connection
            github.checkApiUrlValidity();
        } catch (IOException e) {
            throw new RuntimeException("Error connecting to GitHub: " + e.getMessage(), e);
        }
    }

    /**
     * Constructor with username/password authentication
     * 
     * @param repositoryName The repository name in format "owner/repo"
     * @param username GitHub username
     * @param password GitHub password
     */
    public githubService(String repositoryName, String username, String password) {
        try {
            this.github = new GitHubBuilder().withPassword(username, password).build();
            this.repositoryName = repositoryName;
            // Verify connection
            github.checkApiUrlValidity();
        } catch (IOException e) {
            throw new RuntimeException("Error connecting to GitHub: " + e.getMessage(), e);
        }
    }

    /**
     * Get all issues (open and closed) for the repository
     * 
     * @return List of issues sorted by last updated date descending
     */
    @Override
    public List<Issue> getAllIssues() {
        // Use the Search API to get all issues sorted by updated date descending
        String query = String.format("repo:%s is:issue", repositoryName);
        PagedSearchIterable<GHIssue> searchResults = github.searchIssues()
            .q(query)
            .sort(GHIssueSearchBuilder.Sort.UPDATED)
            .order(GHDirection.DESC)
            .list();

        List<GHIssue> issues = new ArrayList<>();
        for (GHIssue issue : searchResults) {
            issues.add(issue);
        }
        return issues.stream()
                .map(this::convertToIssue)
                .collect(Collectors.toList());
    }
    public List<Issue> searchIssuesSince(String since) {
        String query = String.format("repo:%s updated:>%s", repositoryName, since);
        PagedSearchIterable<GHIssue> searchResults = github.searchIssues()
            .q(query)
            .sort(GHIssueSearchBuilder.Sort.UPDATED)
            .order(GHDirection.DESC)
            .list();
        List<GHIssue> issues = new ArrayList<>();
        for (GHIssue issue : searchResults) {
            issues.add(issue);
        }
        return issues.stream().map(this::convertToIssue).collect(Collectors.toList());
    }
    /**
     * Get all collaborators (users) for the repository
     * 
     * @return List of collaborator usernames
     */
    @Override
    public List<SocialUser> getUsers() {
        try {
            GHRepository repository = github.getRepository(repositoryName);
            List<SocialUser> users = new ArrayList<>();
            
            for (GHUser user : repository.listCollaborators()) {
                SocialUser socialUser = new SocialUser(
                    user.getLogin(),
                    user.getName() != null ? user.getName() : user.getLogin(),
                    user.getEmail()
                );
                users.add(socialUser);
            }
            
            return users;
        } catch (IOException e) {
            throw new RuntimeException("Error retrieving GitHub collaborators: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the URL for a specific issue
     * 
     * @param issueNumber The issue number
     * @return URL of the issue
     */
    @Override
    public String getIssueUrl(int issueNumber) {
        return String.format("https://github.com/%s/issues/%d", repositoryName, issueNumber);
    }

    @Override
    public String getIssuesChannel() {
        String[] parts = repositoryName.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid repository name format. Expected 'owner/repo'.");
        }
        String repo = parts[1];
        return "ghi_"+repo;
    }


    @Override
    public IssueTrackingPlatforms platform() {
        return IssueTrackingPlatforms.jira;
    }

    
    /**
     * Convert a GitHub Issue to our Issue model
     */
    private Issue convertToIssue(GHIssue ghIssue) {
        try {
            String author = ghIssue.getUser().getLogin();
            
            // Get assignees
            List<String> assignees = ghIssue.getAssignees().stream()
                    .map(GHUser::getLogin)
                    .collect(Collectors.toList());
            
            // Get labels
            List<String> labels = ghIssue.getLabels().stream()
                    .map(GHLabel::getName)
                    .collect(Collectors.toList());
            
            // Get comments
            List<IssueComment> comments = new ArrayList<>();
            for (GHIssueComment comment : ghIssue.getComments()) {
                comments.add(new IssueComment(
                    comment.getBody(),
                    comment.getUser().getLogin()
                ));
            }
            
            // Get closed by information
            String closedBy = null;
            if (ghIssue.getState() == GHIssueState.CLOSED && ghIssue.getClosedBy() != null) {
                closedBy = ghIssue.getClosedBy().getLogin();
            }
            Date tsUpdated=ghIssue.getUpdatedAt();
            String stUpdated=tsUpdated!=null?tsUpdated.toInstant().toString():null;
            Date tsCreated=ghIssue.getCreatedAt();
            String stCreated=tsCreated!=null?tsCreated.toInstant().toString():null;
            Date tsCloseat=ghIssue.getClosedAt();
            String stCloseat=tsCloseat!=null?tsCloseat.toInstant().toString():null;
            
            // Create Issue object
            return new Issue(
                    author,
                    assignees,
                    ghIssue.getBody(),
                    stUpdated,
                    stCreated,
                    stCloseat,
                    tsCloseat==null?true:false,
                    closedBy,
                    ghIssue.getHtmlUrl().toString(),
                    labels,
                    ghIssue.getNumber(),
                    repositoryName,
                    ghIssue.getTitle(),
                    comments,
                    List.of() // GitHub doesn't have a direct concept of sub-issues
            );
        } catch (IOException e) {
            throw new RuntimeException("Error converting GitHub issue: " + e.getMessage(), e);
        }
    }

    @Override
    public String getRessourceUrl() {
        return String.format("https://github.com/%s/issues", repositoryName);
    }
    @Override
    public ResTypes resType() {
        return ResTypes.githubissue;
    }


}
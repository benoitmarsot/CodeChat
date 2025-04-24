package com.unbumpkin.codechat.service.issuetracking;

import com.unbumpkin.codechat.dto.issuetracker.AddIssueTrackerRequest;
import com.unbumpkin.codechat.dto.issuetracker.Issue;
import com.unbumpkin.codechat.dto.social.SocialUser;
import com.unbumpkin.codechat.model.ProjectResource.ResTypes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class IssueTrackingService {
    public enum IssueTrackingPlatforms {
        github, jira  //, gitlab, trello, azuredevops
    }

    /**
     * Factory method to get an instance of the appropriate issue tracking service
     * 
     * @param platform The issue tracking platform to use
     * @param repositoryName Repository name or project identifier
     * @param pat Personal Access Token for authentication
     * @return An instance of IssueTrackingService
     */
    public static IssueTrackingService getInstance(IssueTrackingPlatforms platform, String jiraUrl, String userName, String password, String repositoryName, String pat) {
        switch (platform) {
            case github:
                // GitHub repository name should be in the format "owner/repo"
                // Personal Access Token (PAT) is used for authentication
                // PAT should have the necessary scopes for accessing issues
                // Repository name is the full name of the repository (e.g., "owner/repo")
                if(pat == null || pat.isEmpty()) {
                    // If PAT is not provided, use username/password authentication
                    return new githubService(repositoryName, userName, password);
                }
                return new githubService( repositoryName, pat);
            case jira:
                // Jira URL should not end with a slash
                jiraUrl = jiraUrl.endsWith("/") ? jiraUrl.substring(0, jiraUrl.length() - 1) : jiraUrl;
                // Jira username is usually the email address
                // Jira password can be an API token
                // Repository name is the project key in Jira
                return new JiraService( jiraUrl, repositoryName, userName, password);
            // Add other platforms as they're implemented
            default:
                throw new IllegalArgumentException("Unsupported issue tracking platform: " + platform);
        }
    }
    public static IssueTrackingService getInstance(AddIssueTrackerRequest request) {
        return getInstance(request.platform(), request.jiraUrl(), request.userName(), request.password(), request.repositoryName(), request.pat());
    }
    /**
     * Make a user map from the list of users
     * @return Map of usernames to SocialUser objects
     * @throws IOException
     */
    public Map<String, SocialUser> getUsersMap() throws IOException {
        Map<String, SocialUser> userMap = new HashMap<>();
        List<SocialUser> users = getUsers();
        for (SocialUser user : users) {
            userMap.put(user.userId(), user);
        }
        return userMap;
    }


    /**
     * Get the URL for the issue tracking service
     * @return URL of the issue tracking service
     */
    public abstract String getRessourceUrl();

    /**
     * Get all issues for the repository or project
     * 
     * @return List of issues
     */
    public abstract List<Issue> getAllIssues();
    /**
     * Return a channel representation of the issues
     * 
     * @return The channel name or identifier
     */
    public abstract String getIssuesChannel();
    /**
     * Get all issues for the repository or project since a specific date
     * @param since Date in ISO 8601 format (e.g., "2023-01-01T00:00:00Z")
     * @return List of issues
     */
    public abstract List<Issue> searchIssuesSince(String since);
    /**
     * Get all collaborators (users) for the repository or project
     * 
     * @return List of collaborator usernames
     */
    public abstract List<SocialUser> getUsers();
    
    /**
     * Get the URL for a specific issue
     * 
     * @param issueNumber The issue number or identifier
     * @return URL of the issue
     */
    public abstract String getIssueUrl(int issueNumber);
    
    /**
     * Get the resource type for this issue tracking service
     * 
     * @return The resource type enum value
     */
    public abstract ResTypes resType();
    
    /**
     * Get the platform for this issue tracking service
     * 
     * @return The platform enum value
     */
    public abstract IssueTrackingPlatforms platform();
    /**
     * Get the timestamp in ISO 8601 format for a specific issue
     * 
     * @param issue The issue object
     * @return Timestamp in ISO 8601 format
     */
    public String getTSIso8601(String stDate) {
        // Default implementation, can be overridden by subclasses
        return null;
    }
}
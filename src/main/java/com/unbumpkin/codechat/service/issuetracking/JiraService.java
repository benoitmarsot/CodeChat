package com.unbumpkin.codechat.service.issuetracking;

import com.unbumpkin.codechat.dto.issuetracker.Issue;
import com.unbumpkin.codechat.dto.issuetracker.IssueComment;
import com.unbumpkin.codechat.dto.social.SocialUser;
import com.unbumpkin.codechat.model.ProjectResource.ResTypes;

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Comment;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.User;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class JiraService extends IssueTrackingService {
    private final JiraClient jiraClient;
    private final String projectKey;
    private final String jiraUrl;

    /**
     * Constructor with username/password authentication
     * 
     * @param jiraUrl The base URL of the Jira instance (e.g., "https://your-domain.atlassian.net")
     * @param projectKey The Jira project key (e.g., "PROJECT")
     * @param username Jira username (usually email)
     * @param password Jira password or API token
     */
    public JiraService(String jiraUrl, String projectKey, String username, String password) {
        this.jiraUrl = jiraUrl.endsWith("/") ? jiraUrl.substring(0, jiraUrl.length() - 1) : jiraUrl;
        this.projectKey = projectKey;
        
        BasicCredentials credentials = new BasicCredentials(username, password);
        this.jiraClient = new JiraClient(jiraUrl, credentials);
    }

    @Override
    public List<Issue> getAllIssues() {
        try {
            // Query to get all issues for the project
            String jql = "project = " + projectKey + " ORDER BY updated DESC";
            List<net.rcarz.jiraclient.Issue> jiraIssues = jiraClient.searchIssues(jql).issues;
            
            return jiraIssues.stream()
                    .map(this::convertToIssue)
                    .collect(Collectors.toList());
        } catch (JiraException e) {
            throw new RuntimeException("Error retrieving Jira issues: " + e.getMessage(), e);
        }
    }
    @Override 
    public List<Issue> searchIssuesSince(String since) {
        try {
            // Query to get all issues for the project
            String jql = "project = " + projectKey + " AND updated > '" + since + "' ORDER BY updated DESC";
            List<net.rcarz.jiraclient.Issue> jiraIssues = jiraClient.searchIssues(jql).issues;
            
            return jiraIssues.stream()
                    .map(this::convertToIssue)
                    .collect(Collectors.toList());
        } catch (JiraException e) {
            throw new RuntimeException("Error retrieving Jira issues: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SocialUser> getUsers() {
        try {
            // There's no direct API in jira-client to get all users in a project
            // One approach is to get all assignable users for the project
            // This is a simplified implementation and may need adjustment
            
            // Query to get issues and extract unique users
            String jql = "project = " + projectKey;
            List<net.rcarz.jiraclient.Issue> issues = jiraClient.searchIssues(jql).issues;
            
            List<SocialUser> users = new ArrayList<>();
            
            for (net.rcarz.jiraclient.Issue issue : issues) {
                // Add reporter
                User reporter = issue.getReporter();
                if (reporter != null ) {
                    SocialUser user = new SocialUser(
                        reporter.getId(),
                        reporter.getDisplayName(),
                        null // Email is not available in the API
                    );
                    users.add(user);
                }
                
                // Add assignee if present
                User assignee = issue.getAssignee();
                if (assignee != null ) {
                    SocialUser user = new SocialUser(
                        assignee.getId(),
                        assignee.getDisplayName(),
                        null // Email is not available in the API
                    );
                    users.add(user);
                }
                
                // Add comment authors
                if (issue.getComments() != null) {
                    for (Comment comment : issue.getComments()) {
                        User author = comment.getAuthor();
                        SocialUser user = new SocialUser(
                            author.getId(),
                            author.getDisplayName(),
                            null // Email is not available in the API
                        );
                        users.add(user);
                    }
                }
            }
            
            return users;
        } catch (JiraException e) {
            throw new RuntimeException("Error retrieving Jira collaborators: " + e.getMessage(), e);
        }
    }

    @Override
    public String getIssueUrl(int issueNumber) {
        return jiraUrl + "/browse/" + projectKey + "-" + issueNumber;
    }
    @Override
    public String getIssuesChannel() {
        return "ji_"+projectKey;
    }

    @Override
    public IssueTrackingPlatforms platform() {
        return IssueTrackingPlatforms.jira;
    }

    /**
     * Convert a Jira Issue to our Issue model
     */
    private Issue convertToIssue(net.rcarz.jiraclient.Issue jiraIssue) {
        try {
            // Extract author/reporter
            String author = jiraIssue.getReporter() != null ? jiraIssue.getReporter().getName() : "Unknown";
            
            // Extract assignees - Jira typically has a single assignee, convert to list
            List<String> assignees = new ArrayList<>();
            if (jiraIssue.getAssignee() != null) {
                assignees.add(jiraIssue.getAssignee().getName());
            }
            
            // Extract labels
            List<String> labels = jiraIssue.getLabels() != null ? 
                new ArrayList<>(jiraIssue.getLabels()) : new ArrayList<>();
            
            // Extract comments
            List<IssueComment> comments = new ArrayList<>();
            if (jiraIssue.getComments() != null) {
                for (Comment comment : jiraIssue.getComments()) {
                    comments.add(new IssueComment(
                        comment.getBody(),
                        comment.getAuthor().getName()
                    ));
                }
            }
            // Extract subissues 
            List<Issue> subIssues = new ArrayList<>();
            if (jiraIssue.getSubtasks() != null) {
                for (net.rcarz.jiraclient.Issue subIssue : jiraIssue.getSubtasks()) {
                    subIssues.add(convertToIssue(subIssue));
                }
            }
            Date tsUpdated=(Date)jiraIssue.getField("updated");
            String stUpdated=tsUpdated!=null?tsUpdated.toInstant().toString():null;
            Date tsCreated=(Date)jiraIssue.getField("created");
            String stCreated=tsCreated!=null?tsCreated.toInstant().toString():null;
            Date tsCloseat=jiraIssue.getResolutionDate();
            String stCloseat=tsCloseat!=null?tsCloseat.toInstant().toString():null;

            // Extract closeBy information - this is not directly available in Jira API
            // Could be determined from issue history but that's complex
            String closedBy = null;
            int number = jiraIssue.getKey().contains("-") ? 
                Integer.parseInt(jiraIssue.getKey().split("-")[1]) : 0;

            
            // Create an Issue object
            return new Issue(
                author,
                assignees,
                jiraIssue.getDescription(),
                stUpdated,
                stCreated,
                stCloseat,
                stCloseat==null ? true : false,
                closedBy,
                jiraIssue.getUrl(),
                labels,
                number,
                projectKey,
                jiraIssue.getSummary(),
                comments,
                subIssues
            );
        } catch (Exception e) {
            throw new RuntimeException("Error converting Jira issue: " + e.getMessage(), e);
        }
    }

    @Override
    public String getRessourceUrl() {
        return jiraUrl + "/browse/" + projectKey ; 
    }

    @Override
    public ResTypes resType() {
        return ResTypes.jiraissue;
    }
}
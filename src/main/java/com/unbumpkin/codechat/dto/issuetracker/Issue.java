package com.unbumpkin.codechat.dto.issuetracker;

import java.util.List;

public record Issue( 
    String author, List<String> assignees, String body, String lastUpdated, String createdAt, String closedAt, boolean isOpen, String closeBy, 
    String url, List<String> labels, int number, String project, String Title, 
    List<IssueComment> comments, List<Issue> subIssues
) {
        public Issue(Issue issue,String body) {
            this(
                issue.author,
                issue.assignees,
                body,
                issue.lastUpdated,
                issue.createdAt,
                issue.closedAt,
                issue.closedAt==null ? true : false,
                issue.closeBy,
                issue.url,
                issue.labels,
                issue.number,
                issue.project,
                issue.Title,
                issue.comments,
                issue.subIssues
            );
        }

}

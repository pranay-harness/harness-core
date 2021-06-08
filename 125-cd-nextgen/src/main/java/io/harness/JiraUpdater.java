package io.harness;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import net.rcarz.jiraclient.*;

public class JiraUpdater {
  String jiraUrl;
  String username;
  String password;
  String environment;
  String user;
  String buildName;
  String details;
  List<String> serviceNames;
  List<String> jiraIds;
  JiraClient jiraClient;
  public JiraUpdater(String jiraUrl, String username, String password, String environment, String user,
      String buildName, String details, List<String> serviceNames, List<String> jiraIds) throws JiraException {
    //    if (jiraUrl.endsWith("/")) {
    //      // Remove trailing slash
    //      jiraUrl = jiraUrl.substring(0, jiraUrl.length() - 1);
    //    }
    this.jiraUrl = jiraUrl;
    this.username = username;
    this.password = password;
    this.environment = environment;
    this.user = user;
    this.buildName = buildName;
    this.details = details; // Whether the commit was added or removed as part of the deployment
    this.serviceNames = serviceNames;
    BasicCredentials creds = new BasicCredentials(username, password);
    this.jiraClient = new JiraClient(this.jiraUrl, creds);
    this.jiraIds = jiraIds;
  }
  public void Update() {
    // Update comments
    for (String id : this.jiraIds) {
      try {
        updateOrCreateComment(id);
      } catch (JiraException ex) {
        System.err.println("Could not update or create comment for jira ID: " + id + ". Message: " + ex.getMessage());
        if (ex.getCause() != null) {
          System.err.println("Cause for jira ID: " + id + " " + ex.getCause().getMessage());
        }
      }
    }
    // Update labels
    for (String id : this.jiraIds) {
      try {
        if (this.details == "Removed" || this.details == "Rollback" || this.details == "Rolled back") {
          removeLabels(id);
        } else {
          addLabels(id);
        }
      } catch (JiraException ex) {
        System.err.println("Could not update label. Message: " + ex.getMessage());
        if (ex.getCause() != null) {
          System.err.println("Cause: " + ex.getCause().getMessage());
        }
      }
    }
  }
  public void updateOrCreateComment(String jiraId) throws JiraException {
    Comment c = retrieveComment(jiraId);
    if (c == null) {
      c = createNewComment(jiraId);
      System.out.println("Successfully created new comment for jira ID: " + jiraId);
    }
    updateComment(c);
    System.out.println("Successfully updated comment for jira ID: " + jiraId);
  }
  // Retrieve the comment which needs to be updated. This comment will contain the string [Jira Notifier]
  public Comment retrieveComment(String jiraId) throws JiraException {
    Issue issue = this.jiraClient.getIssue(jiraId);
    List<Comment> comments = issue.getComments();
    for (Comment c : comments) {
      if (c.getBody().contains("[Jira Notifier]")) {
        return c;
      }
    }
    return null;
  }
  // Create a new comment with required rows.
  public Comment createNewComment(String jiraId) throws JiraException {
    Issue issue = this.jiraClient.getIssue(jiraId);
    return issue.addComment(
        "[Jira Notifier]\n ||*Environment*||*Deployed by*||*Build Name*||*Timestamp*||*Details*||*Service name*");
  }
  // Update comment with new rows containing the deployment details for each service
  public void updateComment(Comment comment) throws JiraException {
    for (String service : this.serviceNames) {
      String prevBody = comment.getBody();
      String timeStamp = new SimpleDateFormat("yyyy.MM.dd").format(new java.util.Date());
      String newBody = prevBody + "\r\n"
          + String.format(
              "|%s|%s|%s|%s|%s|%s", this.environment, this.user, this.buildName, timeStamp, this.details, service);
      // Update the comment with the new row
      comment.update(newBody);
    }
  }
  public void removeLabels(String jiraId) throws JiraException {
    Issue issue = this.jiraClient.getIssue(jiraId);
    for (String service : this.serviceNames) {
      String label = this.environment + "_" + service.replaceAll("\\s+", "_");
      // Try to remove the label if it exists. Otherwise, continue
      try {
        issue.update().fieldRemove(Field.LABELS, label).execute();
        System.out.println("Removed label: " + label + " for jira ID: " + jiraId);
      } catch (JiraException ex) {
        continue;
      }
    }
  }
  public void addLabels(String jiraId) throws JiraException {
    Issue issue = this.jiraClient.getIssue(jiraId);
    for (String service : this.serviceNames) {
      String label = this.environment + "_" + service.replaceAll("\\s+", "_");
      issue.update()
          .fieldAdd(Field.LABELS, label)
          .execute(); // Add ENV_Service as a label to denote that this environment contains this commit.
      System.out.println("Added label: " + label + " for jira ID: " + jiraId);
    }
  }
  public static void main(String[] args) throws JiraException {
    JiraUpdater jiraUpdater = new JiraUpdater("https://harness.atlassian.net", "vistaar.juneja@harness.io",
        "6BR47FJCM8gi1wVttdjyC5C1", "UAT", "Vistaar Juneja", "releaseBuild-1", "Added",
        Arrays.asList("Log Service", "NG", "Pipeline service"), Arrays.asList("CI-omg", "CI-1801"));
    jiraUpdater.Update();

    JiraUpdater jiraUpdaters = new JiraUpdater("https://harness.atlassian.net", "vistaar.juneja@harness.io",
        "6BR47FJCM8gi1wVttdjyC5C1", "UAT", "Vistaar Juneja", "releaseBuild-1", "Removed", Arrays.asList("Log Service"),
        Arrays.asList("CI-omg", "CI-1801"));

    jiraUpdaters.Update();
  }
}
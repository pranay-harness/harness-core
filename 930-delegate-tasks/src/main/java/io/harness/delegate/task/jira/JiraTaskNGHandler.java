package io.harness.delegate.task.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.jira.mappers.JiraRequestResponseMapper;
import io.harness.exception.InvalidRequestException;
import io.harness.jira.JiraClient;
import io.harness.jira.JiraIssueCreateMetadataNG;
import io.harness.jira.JiraIssueNG;
import io.harness.jira.JiraIssueUpdateMetadataNG;
import io.harness.jira.JiraProjectBasicNG;

import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class JiraTaskNGHandler {
  public JiraTaskNGResponse validateCredentials(JiraTaskNGParameters params) {
    try {
      JiraClient jiraClient = getJiraClient(params);
      jiraClient.getProjects();
      return JiraTaskNGResponse.builder().build();
    } catch (Exception ex) {
      String errorMessage = "Failed to fetch projects during credential validation";
      log.error(errorMessage, ex);
      throw new InvalidRequestException(errorMessage);
    }
  }

  public JiraTaskNGResponse getProjects(JiraTaskNGParameters params) {
    JiraClient jiraClient = getJiraClient(params);
    List<JiraProjectBasicNG> projects = jiraClient.getProjects();
    return JiraTaskNGResponse.builder().projects(projects).build();
  }

  public JiraTaskNGResponse getIssue(JiraTaskNGParameters params) {
    JiraClient jiraClient = getJiraClient(params);
    JiraIssueNG issue = jiraClient.getIssue(params.getIssueKey());
    return JiraTaskNGResponse.builder().issue(issue).build();
  }

  public JiraTaskNGResponse getIssueCreateMetadata(JiraTaskNGParameters params) {
    JiraClient jiraClient = getJiraClient(params);
    JiraIssueCreateMetadataNG createMetadata = jiraClient.getIssueCreateMetadata(
        params.getProjectKey(), params.getIssueType(), params.getExpand(), params.isFetchStatus());
    return JiraTaskNGResponse.builder().issueCreateMetadata(createMetadata).build();
  }

  public JiraTaskNGResponse getIssueUpdateMetadata(JiraTaskNGParameters params) {
    JiraClient jiraClient = getJiraClient(params);
    JiraIssueUpdateMetadataNG updateMetadata = jiraClient.getIssueUpdateMetadata(params.getIssueKey());
    return JiraTaskNGResponse.builder().issueUpdateMetadata(updateMetadata).build();
  }

  public JiraTaskNGResponse createIssue(JiraTaskNGParameters params) {
    JiraClient jiraClient = getJiraClient(params);
    JiraIssueNG issue = jiraClient.createIssue(params.getProjectKey(), params.getIssueType(), params.getFields());
    return JiraTaskNGResponse.builder().issue(issue).build();
  }

  public JiraTaskNGResponse updateIssue(JiraTaskNGParameters params) {
    JiraClient jiraClient = getJiraClient(params);
    JiraIssueNG issue = jiraClient.updateIssue(
        params.getIssueKey(), params.getTransitionToStatus(), params.getTransitionName(), params.getFields());
    return JiraTaskNGResponse.builder().issue(issue).build();
  }

  private JiraClient getJiraClient(JiraTaskNGParameters parameters) {
    return new JiraClient(JiraRequestResponseMapper.toJiraInternalConfig(parameters));
  }
}

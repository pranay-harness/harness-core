package io.harness.delegate.task.jira;

import com.google.inject.Singleton;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import lombok.extern.slf4j.Slf4j;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

@Singleton
@Slf4j
public class JiraTaskNGHandler {
  public JiraTaskNGResponse validateCredentials(JiraTaskNGParameters jiraTaskNGParameters) {
    try {
      JiraClient jiraClient = getJiraClient(jiraTaskNGParameters);
      jiraClient.getProjects();
    } catch (JiraException e) {
      String errorMessage = "Failed to fetch projects during credential validation.";
      logger.error(errorMessage, e);
      return JiraTaskNGResponse.builder().errorMessage(errorMessage).executionStatus(FAILURE).build();
    }

    return JiraTaskNGResponse.builder().executionStatus(SUCCESS).build();
  }

  private JiraClient getJiraClient(JiraTaskNGParameters parameters) throws JiraException {
    JiraConnectorDTO jiraConnectorDTO = parameters.getJiraConnectorDTO();
    BasicCredentials creds = new BasicCredentials(
        jiraConnectorDTO.getUsername(), String.valueOf(jiraConnectorDTO.getPasswordRef().getDecryptedValue()));
    String jiraUrl = jiraConnectorDTO.getJiraUrl();

    String baseUrl = jiraUrl.endsWith("/") ? jiraUrl : jiraUrl.concat("/");
    return new JiraClient(baseUrl, creds);
  }
}
package software.wings.service.impl;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.JiraConfig;
import software.wings.dl.WingsPersistence;

/**
 * All Jira apis should be accessed via this object.
 */
public class JiraHelperService {
  private static final Logger logger = LoggerFactory.getLogger(GcpHelperService.class);
  @Inject WingsPersistence wingsPersistence;

  /**
   * Validate credential.
   *
   */
  public void validateCredential(JiraConfig jiraConfig) {
    BasicCredentials creds = new BasicCredentials(jiraConfig.getUsername(), new String(jiraConfig.getPassword()));
    JiraClient jira = new JiraClient(jiraConfig.getBaseUrl(), creds);

    try {
      jira.getProjects();

    } catch (JiraException e) {
      logger.error("[JIRA] Invalid url or credentials");
      logger.info(e.getMessage());

      throw new InvalidRequestException(
          "Failed to Authenticate with JIRA Server. " + extractRelevantMessage(e.getMessage()));
    }
  }

  private String extractRelevantMessage(String message) {
    String[] words = message.split("\\s+");

    return words[0] + " " + words[1];
  }
}

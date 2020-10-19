package io.harness.delegate.task.jira;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.security.encryption.SecretDecryptionService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import static io.harness.logging.CommandExecutionStatus.FAILURE;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class JiraTaskNGHelper {
  private final JiraTaskNGHandler jiraTaskNGHandler;
  private final SecretDecryptionService secretDecryptionService;

  public JiraTaskNGResponse getJiraTaskResponse(JiraTaskNGParameters taskParameters) {
    JiraTaskNGResponse responseData;
    decryptRequestDTOs(taskParameters);
    switch (taskParameters.getJiraAction()) {
      case AUTH:
        responseData = jiraTaskNGHandler.validateCredentials(taskParameters);
        break;
      case CREATE_TICKET:
        throw new NotImplementedException(taskParameters.getJiraAction().toString() + " is not implemented");
      default:
        logger.error("No corresponding Docker artifact task type [{}]", taskParameters.toString());
        return JiraTaskNGResponse.builder()
            .executionStatus(FAILURE)
            .jiraAction(taskParameters.getJiraAction())
            .errorMessage("There is no such jira action - " + taskParameters.getJiraAction().name())
            .build();
    }
    return responseData;
  }

  private void decryptRequestDTOs(JiraTaskNGParameters jiraTaskNGParameters) {
    secretDecryptionService.decrypt(
        jiraTaskNGParameters.getJiraConnectorDTO(), jiraTaskNGParameters.getEncryptionDetails());
  }
}
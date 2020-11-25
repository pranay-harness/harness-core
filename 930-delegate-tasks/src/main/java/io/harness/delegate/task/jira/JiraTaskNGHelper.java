package io.harness.delegate.task.jira;

import static io.harness.logging.CommandExecutionStatus.FAILURE;

import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        responseData = jiraTaskNGHandler.createTicket(taskParameters);
        break;
      case UPDATE_TICKET:
        responseData = jiraTaskNGHandler.updateTicket(taskParameters);
        break;
      case FETCH_ISSUE:
        responseData = jiraTaskNGHandler.fetchIssue(taskParameters);
        break;
      case GET_PROJECTS:
        responseData = jiraTaskNGHandler.getProjects(taskParameters);
        break;
      case GET_STATUSES:
        responseData = jiraTaskNGHandler.getStatuses(taskParameters);
        break;
      case GET_FIELDS_OPTIONS:
        responseData = jiraTaskNGHandler.getFieldsOptions(taskParameters);
        break;
      case CHECK_APPROVAL:
        responseData = jiraTaskNGHandler.checkJiraApproval(taskParameters);
        break;
      case GET_CREATE_METADATA:
        responseData = jiraTaskNGHandler.getCreateMetadata(taskParameters);
        break;
      default:
        log.error("No corresponding Jira action task type [{}]", taskParameters.toString());
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

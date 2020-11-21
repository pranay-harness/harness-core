package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.jira.JiraConnectionTaskParams;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.connector.jira.connection.JiraTestConnectionTaskNGResponse;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class JiraConnectorValidator
    extends AbstractConnectorValidator implements ConnectionValidator<JiraConnectorDTO> {
  @Override
  public ConnectorValidationResult validate(
      JiraConnectorDTO jiraConnectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    JiraTestConnectionTaskNGResponse delegateResponseData = (JiraTestConnectionTaskNGResponse) super.validateConnector(
        jiraConnectorDTO, accountIdentifier, orgIdentifier, projectIdentifier);
    return ConnectorValidationResult.builder()
        .valid(delegateResponseData.getCanConnect())
        .errorMessage(delegateResponseData.getErrorMessage())
        .build();
  }

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    JiraConnectorDTO jiraConnectorDTO = (JiraConnectorDTO) connectorConfig;

    return JiraConnectionTaskParams.builder()
        .jiraConnectorDTO(jiraConnectorDTO)
        .encryptionDetails(
            super.getEncryptionDetail(jiraConnectorDTO, accountIdentifier, orgIdentifier, projectIdentifier))
        .build();
  }

  @Override
  public String getTaskType() {
    return "JIRA_CONNECTIVITY_TASK_NG";
  }
}

package io.harness.connector.validator.scmValidators;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static software.wings.beans.TaskType.NG_AWS_CODE_COMMIT_TASK;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.validator.AbstractConnectorValidator;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.awscodecommitconnector.AwsCodeCommitTaskParams;
import io.harness.delegate.beans.connector.awsconnector.AwsValidateTaskResponse;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.task.TaskParameters;

import java.util.List;

@OwnedBy(DX)
public class AwsCodeCommitValidator extends AbstractConnectorValidator {
  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    AwsCodeCommitConnectorDTO connectorDTO = (AwsCodeCommitConnectorDTO) connectorConfig;
    final List<DecryptableEntity> decryptableEntities = connectorDTO.getDecryptableEntities();
    return AwsCodeCommitTaskParams.builder()
        .awsConnector(connectorDTO)
        .encryptionDetails(
            super.getEncryptionDetail(decryptableEntities.get(0), accountIdentifier, orgIdentifier, projectIdentifier))
        .build();
  }

  @Override
  public String getTaskType() {
    return NG_AWS_CODE_COMMIT_TASK.name();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    AwsValidateTaskResponse responseData = (AwsValidateTaskResponse) super.validateConnector(
        connectorDTO, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return responseData.getConnectorValidationResult();
  }
}

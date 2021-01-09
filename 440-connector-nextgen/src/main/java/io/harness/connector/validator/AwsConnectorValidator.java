package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskParams;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskType;
import io.harness.delegate.beans.connector.awsconnector.AwsValidateTaskResponse;
import io.harness.delegate.task.TaskParameters;

public class AwsConnectorValidator extends AbstractConnectorValidator implements ConnectionValidator<AwsConnectorDTO> {
  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    AwsConnectorDTO connectorDTO = (AwsConnectorDTO) connectorConfig;
    final AwsManualConfigSpecDTO awsCredentialDTO =
        connectorDTO.getCredential().getAwsCredentialType() == AwsCredentialType.MANUAL_CREDENTIALS
        ? ((AwsManualConfigSpecDTO) connectorDTO.getCredential().getConfig())
        : null;
    return AwsTaskParams.builder()
        .awsTaskType(AwsTaskType.VALIDATE)
        .awsConnector(connectorDTO)
        .encryptionDetails(
            super.getEncryptionDetail(awsCredentialDTO, accountIdentifier, orgIdentifier, projectIdentifier))
        .build();
  }

  @Override
  public String getTaskType() {
    return "NG_AWS_TASK";
  }

  @Override
  public ConnectorValidationResult validate(
      AwsConnectorDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    AwsValidateTaskResponse responseData = (AwsValidateTaskResponse) super.validateConnector(
        connectorDTO, accountIdentifier, orgIdentifier, projectIdentifier);
    return responseData.getConnectorValidationResult();
  }
}

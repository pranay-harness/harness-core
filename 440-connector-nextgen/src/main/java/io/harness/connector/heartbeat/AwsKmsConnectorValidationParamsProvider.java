package io.harness.connector.heartbeat;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsValidationParams;

public class AwsKmsConnectorValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorConfigDTO,
      String connectorName, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return AwsKmsValidationParams.builder()
        .awsKmsConnectorDTO((AwsConnectorDTO) connectorConfigDTO.getConnectorConfig())
        .connectorName(connectorName)
        .build();
  }
}

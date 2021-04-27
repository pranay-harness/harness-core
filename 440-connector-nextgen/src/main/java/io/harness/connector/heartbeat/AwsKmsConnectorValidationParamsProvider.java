package io.harness.connector.heartbeat;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsValidationParams;

import com.google.inject.Singleton;

@OwnedBy(PL)
@Singleton
public class AwsKmsConnectorValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorConfigDTO,
      String connectorName, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return AwsKmsValidationParams.builder()
        .awsKmsConnectorDTO((AwsKmsConnectorDTO) connectorConfigDTO.getConnectorConfig())
        .connectorName(connectorName)
        .build();
  }
}

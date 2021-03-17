package io.harness.delegate.beans.connector.artifactoryconnector;

import io.harness.delegate.beans.connector.ConnectorTaskParams;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class ArtifactoryValidationParams
    extends ConnectorTaskParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  ArtifactoryConnectorDTO artifactoryConnectorDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
  String connectorName;

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.ARTIFACTORY;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return ArtifactoryCapabilityHelper.fetchRequiredExecutionCapabilities(artifactoryConnectorDTO, maskingEvaluator);
  }
}

package io.harness.delegate.beans.connector.docker;

import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DockerValidationParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  DockerConnectorDTO dockerConnectorDTO;
  List<EncryptedDataDetail> encryptionDataDetails;
  String connectorName;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return DockerCapabilityHelper.fetchRequiredExecutionCapabilities(dockerConnectorDTO, maskingEvaluator);
  }

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.DOCKER;
  }
}

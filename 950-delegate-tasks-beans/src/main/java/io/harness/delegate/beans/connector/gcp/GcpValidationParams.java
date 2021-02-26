package io.harness.delegate.beans.connector.gcp;

import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GcpValidationParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  GcpConnectorDTO gcpConnectorDTO;
  private List<EncryptedDataDetail> encryptionDetails;
  String connectorName;
  private Set<String> delegateSelectors;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return GcpCapabilityHelper.fetchRequiredExecutionCapabilities(gcpConnectorDTO, maskingEvaluator);
  }

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.GCP;
  }
}

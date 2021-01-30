package io.harness.delegate.beans.connector.vaultconnector;

import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VaultValidationParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  VaultConnectorDTO vaultConnectorDTO;
  String connectorName;

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.VAULT;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return vaultConnectorDTO.fetchRequiredExecutionCapabilities(maskingEvaluator);
  }
}

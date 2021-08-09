package io.harness.delegate.beans.connector.vaultconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper.populateDelegateSelectorCapability;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
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
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    if (vaultConnectorDTO != null) {
      executionCapabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
          vaultConnectorDTO.getVaultUrl(), maskingEvaluator));
      populateDelegateSelectorCapability(executionCapabilities, vaultConnectorDTO.getDelegateSelectors());
    }
    return executionCapabilities;
  }
}

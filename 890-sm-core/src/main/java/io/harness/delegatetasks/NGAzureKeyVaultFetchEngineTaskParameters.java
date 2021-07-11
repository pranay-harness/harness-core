package io.harness.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.azurekeyvaultconnector.AzureKeyVaultCapabilityHelper;
import io.harness.delegate.beans.connector.azurekeyvaultconnector.AzureKeyVaultConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@OwnedBy(PL)
@Getter
@Builder
public class NGAzureKeyVaultFetchEngineTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private final AzureKeyVaultConnectorDTO azureKeyVaultConnectorDTO;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return AzureKeyVaultCapabilityHelper.fetchRequiredExecutionCapabilities(
        azureKeyVaultConnectorDTO, maskingEvaluator);
  }
}

package io.harness.delegate.beans.connector.azurekeyvaultconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.delegate.beans.connector.azurekeyvaultconnector.AzureKeyVaultConstants.AZURE_DEFAULT_ENCRYPTION_URL;
import static io.harness.delegate.beans.connector.azurekeyvaultconnector.AzureKeyVaultConstants.AZURE_US_GOVERNMENT_ENCRYPTION_URL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class AzureKeyVaultValidationParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  AzureKeyVaultConnectorDTO azurekeyvaultConnectorDTO;
  String connectorName;

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.AZURE_KEY_VAULT;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    if (azurekeyvaultConnectorDTO != null) {
      String encryptionServiceUrl;
      if (azurekeyvaultConnectorDTO.getAzureEnvironmentType() == null) {
        encryptionServiceUrl = String.format(AZURE_DEFAULT_ENCRYPTION_URL, azurekeyvaultConnectorDTO.getVaultName());
      } else {
        switch (azurekeyvaultConnectorDTO.getAzureEnvironmentType()) {
          case AZURE_US_GOVERNMENT:
            encryptionServiceUrl =
                String.format(AZURE_US_GOVERNMENT_ENCRYPTION_URL, azurekeyvaultConnectorDTO.getVaultName());
            break;
          case AZURE:
          default:
            encryptionServiceUrl =
                String.format(AZURE_DEFAULT_ENCRYPTION_URL, azurekeyvaultConnectorDTO.getVaultName());
        }
      }

      return Collections.singletonList(
          HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
              encryptionServiceUrl, maskingEvaluator));
    }
    return Collections.emptyList();
  }
}

package io.harness.delegate.beans.connector.azurekeyvaultconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.delegate.beans.connector.azurekeyvaultconnector.AzureKeyVaultConstants.AZURE_DEFAULT_ENCRYPTION_URL;
import static io.harness.delegate.beans.connector.azurekeyvaultconnector.AzureKeyVaultConstants.AZURE_US_GOVERNMENT_ENCRYPTION_URL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AzureKeyVaultCapabilityHelper extends ConnectorCapabilityBaseHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      AzureKeyVaultConnectorDTO azurekeyvaultConnectorDTO, ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
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

      executionCapabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
          encryptionServiceUrl, maskingEvaluator));
      populateDelegateSelectorCapability(executionCapabilities, azurekeyvaultConnectorDTO.getDelegateSelectors());
    }
    return executionCapabilities;
  }
}

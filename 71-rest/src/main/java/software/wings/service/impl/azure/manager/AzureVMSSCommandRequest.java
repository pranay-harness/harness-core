package software.wings.service.impl.azure.manager;

import io.harness.delegate.beans.azure.AzureConfigDelegate;
import io.harness.delegate.beans.azure.AzureVMAuthDelegate;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import lombok.Builder;
import lombok.Data;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class AzureVMSSCommandRequest implements TaskParameters, ExecutionCapabilityDemander {
  private AzureConfigDelegate azureConfigDelegate;
  private AzureVMAuthDelegate azureHostConnectionDelegate;
  private AzureVMAuthDelegate azureVMCredentialsDelegate;
  private AzureVMSSTaskParameters azureVMSSTaskParameters;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    Set<ExecutionCapability> executionCapabilities = new HashSet<>(CapabilityHelper.generateDelegateCapabilities(
        azureConfigDelegate.getAzureConfigDTO(), azureConfigDelegate.getAzureEncryptionDetails()));

    if (azureHostConnectionDelegate != null && azureHostConnectionDelegate.getAzureVMAuthDTO() != null
        && azureHostConnectionDelegate.getAzureEncryptionDetails() != null) {
      executionCapabilities.addAll(CapabilityHelper.generateDelegateCapabilities(
          azureHostConnectionDelegate.getAzureVMAuthDTO(), azureHostConnectionDelegate.getAzureEncryptionDetails()));
    }

    if (azureVMCredentialsDelegate != null && azureVMCredentialsDelegate.getAzureVMAuthDTO() != null
        && azureVMCredentialsDelegate.getAzureEncryptionDetails() != null) {
      executionCapabilities.addAll(CapabilityHelper.generateDelegateCapabilities(
          azureVMCredentialsDelegate.getAzureVMAuthDTO(), azureVMCredentialsDelegate.getAzureEncryptionDetails()));
    }
    return new ArrayList<>(executionCapabilities);
  }
}

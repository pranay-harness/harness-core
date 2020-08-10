package software.wings.service.impl.azure.manager;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.AzureConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.ServiceVariable;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class AzureVMSSCommandRequest implements TaskParameters, ExecutionCapabilityDemander {
  private AzureConfig azureConfig;
  private List<EncryptedDataDetail> azureEncryptionDetails;
  private HostConnectionAttributes hostConnectionAttributes;
  private List<EncryptedDataDetail> hostConnectionAttributesEncryptionDetails;
  private ServiceVariable serviceVariable;
  private List<EncryptedDataDetail> serviceVariableEncryptionDetails;
  private AzureVMSSTaskParameters azureVMSSTaskParameters;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    Set<ExecutionCapability> executionCapabilities = new HashSet<>();
    executionCapabilities.addAll(CapabilityHelper.generateDelegateCapabilities(azureConfig, azureEncryptionDetails));

    if (hostConnectionAttributes != null && hostConnectionAttributesEncryptionDetails != null) {
      executionCapabilities.addAll(CapabilityHelper.generateDelegateCapabilities(
          hostConnectionAttributes, hostConnectionAttributesEncryptionDetails));
    }

    if (serviceVariableEncryptionDetails != null) {
      executionCapabilities.addAll(
          CapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(serviceVariableEncryptionDetails));
    }
    return new ArrayList<>(executionCapabilities);
  }
}

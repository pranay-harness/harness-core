package io.harness.delegate.task.azure;

import io.harness.delegate.beans.azure.AzureConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class AzureTaskExecutionRequest implements TaskParameters, ExecutionCapabilityDemander {
  private AzureConfigDTO azureConfigDTO;
  private List<EncryptedDataDetail> azureConfigEncryptionDetails;
  private AzureTaskParameters azureTaskParameters;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    Set<ExecutionCapability> executionCapabilities = new HashSet<>();
    return new ArrayList<>(executionCapabilities);
  }
}

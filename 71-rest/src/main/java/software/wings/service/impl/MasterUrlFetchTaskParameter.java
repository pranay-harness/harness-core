package software.wings.service.impl;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;

import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class MasterUrlFetchTaskParameter implements TaskParameters, ExecutionCapabilityDemander {
  ContainerServiceParams containerServiceParams;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
        containerServiceParams.getEncryptionDetails());
  }
}

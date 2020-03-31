package software.wings.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Data;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import java.util.List;

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

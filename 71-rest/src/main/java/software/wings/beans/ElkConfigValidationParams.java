package software.wings.beans;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import java.util.List;

@Value
@Builder
public class ElkConfigValidationParams implements TaskParameters, ExecutionCapabilityDemander {
  private ElkConfig elkConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(elkConfig, encryptedDataDetails);
  }
}

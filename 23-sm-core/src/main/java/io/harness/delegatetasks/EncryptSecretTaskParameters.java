package io.harness.delegatetasks;

import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptionConfig;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EncryptSecretTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private final String value;
  private final EncryptionConfig encryptionConfig;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return ((SecretManagerConfig) encryptionConfig).fetchRequiredExecutionCapabilities();
  }
}

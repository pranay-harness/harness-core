package software.wings.service.impl.stackdriver;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.GcpConfig;

import java.util.List;
@Value
@Builder
public class StackdriverLogGcpConfigTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  private GcpConfig gcpConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return StackdriverUtils.fetchRequiredExecutionCapabilitiesForLogs(encryptedDataDetails);
  }
}

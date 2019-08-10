package software.wings.service.impl.spotinst;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class SpotInstCommandRequest implements TaskParameters, ExecutionCapabilityDemander {
  private AwsConfig awsConfig;
  private SpotInstConfig spotInstConfig;
  private List<EncryptedDataDetail> awsEncryptionDetails;
  private List<EncryptedDataDetail> spotinstEncryptionDetails;
  private SpotInstTaskParameters spotInstTaskParameters;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    Set<ExecutionCapability> executionCapabilities = new HashSet<>();
    executionCapabilities.addAll(CapabilityHelper.generateDelegateCapabilities(awsConfig, awsEncryptionDetails));
    executionCapabilities.addAll(
        CapabilityHelper.generateDelegateCapabilities(spotInstConfig, spotinstEncryptionDetails));
    return new ArrayList<>(executionCapabilities);
  }
}

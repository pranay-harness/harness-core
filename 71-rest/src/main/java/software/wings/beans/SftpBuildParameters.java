package software.wings.beans;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import java.util.List;

@Value
@Builder
public class SftpBuildParameters implements TaskParameters, ExecutionCapabilityDemander {
  private String appId;
  private ArtifactStreamAttributes artifactStreamAttributes;
  private SftpConfig sftpConfig;
  private List<EncryptedDataDetail> encryptionDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilities =
        CapabilityHelper.generateCapabilities(sftpConfig, artifactStreamAttributes);
    executionCapabilities.addAll(CapabilityHelper.generateKmsHttpCapabilities(encryptionDetails));
    return executionCapabilities;
  }
}

package software.wings.delegatetasks.collect.artifacts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Value
@Builder
public class AzureArtifactsCollectionTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  @NotNull private String accountId;
  @NotNull private AzureArtifactsConfig azureArtifactsConfig;
  @NotNull private List<EncryptedDataDetail> encryptedDataDetails;
  @NotNull private ArtifactStreamAttributes artifactStreamAttributes;
  private Map<String, String> artifactMetadata;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    if (azureArtifactsConfig instanceof AzureArtifactsPATConfig) {
      return CapabilityHelper.generateCapabilities(
          (AzureArtifactsPATConfig) azureArtifactsConfig, artifactStreamAttributes);
    } else {
      throw new InvalidRequestException("Invalid Azure Artifacts Server config");
    }
  }
}

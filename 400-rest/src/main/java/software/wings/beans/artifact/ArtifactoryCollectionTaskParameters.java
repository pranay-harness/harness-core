package software.wings.beans.artifact;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.config.ArtifactoryConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(HarnessTeam.DEL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactoryCollectionTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private ArtifactoryConfig artifactoryConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private String jobName;
  private Map<String, String> metadata;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return CapabilityHelper.generateDelegateCapabilities(artifactoryConfig, encryptedDataDetails, maskingEvaluator);
  }
}

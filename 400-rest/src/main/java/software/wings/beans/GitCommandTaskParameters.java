package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.yaml.GitCommand.GitCommandType;
import software.wings.beans.yaml.GitCommandRequest;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(HarnessTeam.DX)
@Data
@Builder
public class GitCommandTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private final GitCommandType gitCommandType;
  private final GitConfig gitConfig;
  private final List<EncryptedDataDetail> encryptedDataDetails;
  private final GitCommandRequest gitCommandRequest;
  private final Boolean excludeFilesOutsideSetupFolder;

  public static GitCommandTaskParametersBuilder builder(
      GitCommandType gitCommandType, GitConfig gitConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return new GitCommandTaskParametersBuilder()
        .gitCommandType(gitCommandType)
        .gitConfig(gitConfig)
        .encryptedDataDetails(encryptedDataDetails);
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return CapabilityHelper.generateDelegateCapabilities(gitConfig, encryptedDataDetails, maskingEvaluator);
  }
}

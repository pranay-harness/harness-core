package io.harness.delegate.task.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.capability.ProcessExecutionCapabilityHelper;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.terraform.TFTaskType;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionReflectionUtils;
import io.harness.yaml.core.variables.NGVariable;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
public class AwsSamTaskParameters
    implements TaskParameters, ExecutionCapabilityDemander, ExpressionReflectionUtils.NestedAnnotationResolver {
  @NonNull String accountId;
  @NonNull TFTaskType taskType;
  @NonNull String entityId;
  @NonNull GitFetchFilesConfig configFile;
  @NonNull String region;
  @NonNull String stackName;
  @NonNull AwsSamTaskType awsSamTaskType;
  List<NGVariable> overrides;
  String globalAdditionalFlags;

  // To help in logging
  AwsSamCommandUnit awsSamCommandUnit;

  long timeoutInMillis;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilities = ProcessExecutionCapabilityHelper.generateExecutionCapabilitiesForTerraform(
        configFile.getGitStoreDelegateConfig().getEncryptedDataDetails(), maskingEvaluator);
    if (configFile != null) {
      capabilities.add(GitConnectionNGCapability.builder()
                           .gitConfig((GitConfigDTO) configFile.getGitStoreDelegateConfig().getGitConfigDTO())
                           .encryptedDataDetails(configFile.getGitStoreDelegateConfig().getEncryptedDataDetails())
                           .sshKeySpecDTO(configFile.getGitStoreDelegateConfig().getSshKeySpecDTO())
                           .build());
    }

    return capabilities;
  }
}

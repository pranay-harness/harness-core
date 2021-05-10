package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._950_DELEGATE_TASKS_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;

import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.delegatetasks.validation.capabilities.GitConnectionCapability;
import software.wings.service.impl.ContainerServiceParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(_950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class GitFetchFilesTaskParams implements ActivityAccess, TaskParameters, ExecutionCapabilityDemander {
  private String accountId;
  private String appId;
  private String activityId;
  private boolean isFinalState;
  private AppManifestKind appManifestKind;
  private Map<String, GitFetchFilesConfig> gitFetchFilesConfigMap;
  private final ContainerServiceParams containerServiceParams;
  private boolean isBindTaskFeatureSet; // BIND_FETCH_FILES_TASK_TO_DELEGATE
  private String executionLogName;
  private Set<String> delegateSelectors;
  private boolean isGitHostConnectivityCheck;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();

    if (isBindTaskFeatureSet && containerServiceParams != null) {
      executionCapabilities.addAll(containerServiceParams.fetchRequiredExecutionCapabilities(maskingEvaluator));
    }

    if (isNotEmpty(gitFetchFilesConfigMap)) {
      if (isGitHostConnectivityCheck) {
        for (Map.Entry<String, GitFetchFilesConfig> entry : gitFetchFilesConfigMap.entrySet()) {
          GitFetchFilesConfig gitFetchFileConfig = entry.getValue();
          executionCapabilities.addAll(
              CapabilityHelper.generateExecutionCapabilitiesForGit(gitFetchFileConfig.getGitConfig()));
        }
      } else {
        for (Map.Entry<String, GitFetchFilesConfig> entry : gitFetchFilesConfigMap.entrySet()) {
          GitFetchFilesConfig gitFetchFileConfig = entry.getValue();
          executionCapabilities.add(GitConnectionCapability.builder()
                                        .gitConfig(gitFetchFileConfig.getGitConfig())
                                        .settingAttribute(gitFetchFileConfig.getGitConfig().getSshSettingAttribute())
                                        .encryptedDataDetails(gitFetchFileConfig.getEncryptedDataDetails())
                                        .build());
        }
      }
    }

    if (isNotEmpty(delegateSelectors)) {
      executionCapabilities.add(SelectorCapability.builder().selectors(delegateSelectors).build());
    }

    return executionCapabilities;
  }
}

package io.harness.cdng.tasks.manifestFetch.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Data;
import software.wings.delegatetasks.validation.capabilities.GitConnectionCapability;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class GitFetchRequest implements ActivityAccess, TaskParameters, ExecutionCapabilityDemander {
  private List<GitFetchFilesConfig> gitFetchFilesConfigs;
  private String executionLogName;
  private String activityId;
  private String accountId;
  private String appId;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();

    if (isNotEmpty(gitFetchFilesConfigs)) {
      for (GitFetchFilesConfig gitFetchFileConfig : gitFetchFilesConfigs) {
        executionCapabilities.add(GitConnectionCapability.builder()
                                      .gitConfig(gitFetchFileConfig.getGitConfig())
                                      .settingAttribute(gitFetchFileConfig.getGitConfig().getSshSettingAttribute())
                                      .encryptedDataDetails(gitFetchFileConfig.getEncryptedDataDetails())
                                      .build());
      }
    }

    return executionCapabilities;
  }
}

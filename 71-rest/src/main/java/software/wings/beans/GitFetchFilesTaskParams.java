package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.service.impl.ContainerServiceParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
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

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();

    if (isBindTaskFeatureSet && containerServiceParams != null) {
      executionCapabilities.addAll(containerServiceParams.fetchRequiredExecutionCapabilities());
    }

    if (isNotEmpty(gitFetchFilesConfigMap)) {
      for (Map.Entry<String, GitFetchFilesConfig> entry : gitFetchFilesConfigMap.entrySet()) {
        GitFetchFilesConfig gitFetchFileConfig = entry.getValue();
        executionCapabilities.addAll(gitFetchFileConfig.getGitConfig().fetchRequiredExecutionCapabilities());
      }
    }

    return executionCapabilities;
  }
}

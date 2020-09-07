package io.harness.delegate.task.git;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@Builder
public class GitFetchRequest implements ActivityAccess, TaskParameters, ExecutionCapabilityDemander {
  private List<GitFetchFilesConfig> gitFetchFilesConfigs;
  private String executionLogName;
  private String activityId;
  private String accountId;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    // TODO VS/Anshul: add capability later
    return Collections.emptyList();
  }
}

package io.harness.delegate.task.git;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitFetchRequest implements ActivityAccess, TaskParameters, ExecutionCapabilityDemander {
  private List<GitFetchFilesConfig> gitFetchFilesConfigs;
  private String executionLogName;
  private String activityId;
  private String accountId;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    // TODO VS/Anshul: add capability later
    return Collections.emptyList();
  }
}

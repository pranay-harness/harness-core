package io.harness.delegate.task.artifacts.request;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.artifacts.ArtifactSourceDelegateRequest;
import io.harness.delegate.task.artifacts.ArtifactTaskType;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtifactTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  @NotNull String accountId;
  @NotNull ArtifactSourceDelegateRequest attributes;
  @NotNull ArtifactTaskType artifactTaskType;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return attributes.fetchRequiredExecutionCapabilities();
  }
}

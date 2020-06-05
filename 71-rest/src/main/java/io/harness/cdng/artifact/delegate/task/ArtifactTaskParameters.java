package io.harness.cdng.artifact.delegate.task;

import io.harness.cdng.artifact.bean.artifactsource.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.ConnectorConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class ArtifactTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  @NotNull String accountId;
  @NotNull String appId;
  @NotNull ArtifactSourceAttributes attributes;
  @NotNull ConnectorConfig connectorConfig;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return null;
  }
}

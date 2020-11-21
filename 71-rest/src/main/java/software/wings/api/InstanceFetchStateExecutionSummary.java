package software.wings.api;

import static java.util.Collections.singletonList;

import software.wings.service.impl.instance.DeploymentInfoExtractor;
import software.wings.sm.StepExecutionSummary;

import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class InstanceFetchStateExecutionSummary extends StepExecutionSummary implements DeploymentInfoExtractor {
  private String instanceFetchScript;
  private String scriptOutput;
  private String activityId;

  @Override
  public Optional<List<DeploymentInfo>> extractDeploymentInfo() {
    return Optional.of(singletonList(CustomDeploymentTypeInfo.builder()
                                         .scriptOutput(scriptOutput)
                                         .instanceFetchScript(instanceFetchScript)
                                         .build()));
  }
}

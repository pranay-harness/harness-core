package io.harness.steps.common.steps.stepgroup;

import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.steps.StepGroupElementConfig;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@TypeAlias("stepGroupStepParameters")
@EqualsAndHashCode(callSuper = true)
public class StepGroupStepParameters extends StepGroupElementConfig implements StepParameters {
  String childNodeID;

  @Builder(builderMethodName = "newBuilder")
  public StepGroupStepParameters(String uuid, String identifier, String name,
      List<FailureStrategyConfig> failureStrategies, List<ExecutionWrapperConfig> steps,
      List<ExecutionWrapperConfig> rollbackSteps, String childNodeID) {
    super(uuid, identifier, name, failureStrategies, steps, rollbackSteps);
    this.childNodeID = childNodeID;
  }

  public static StepGroupStepParameters getStepParameters(StepGroupElementConfig config, String childNodeID) {
    if (config == null) {
      return StepGroupStepParameters.newBuilder().childNodeID(childNodeID).build();
    }
    return StepGroupStepParameters.newBuilder()
        .name(config.getName())
        .identifier(config.getIdentifier())
        .steps(config.getSteps())
        .failureStrategies(config.getFailureStrategies())
        .rollbackSteps(config.getRollbackSteps())
        .childNodeID(childNodeID)
        .build();
  }
}

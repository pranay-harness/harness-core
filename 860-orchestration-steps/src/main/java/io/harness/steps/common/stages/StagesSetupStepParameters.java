package io.harness.steps.common.stages;

import io.harness.plancreator.stages.StageElementWrapperConfig;
import io.harness.plancreator.stages.StagesConfig;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@EqualsAndHashCode(callSuper = true)
@TypeAlias("stagesSetupStepParameters")
public class StagesSetupStepParameters extends StagesConfig implements StepParameters {
  Set<String> childrenNodeIds;

  @Builder(builderMethodName = "newBuilder")
  public StagesSetupStepParameters(List<StageElementWrapperConfig> stages, Set<String> childrenNodeIds) {
    super(stages);
    this.childrenNodeIds = childrenNodeIds;
  }

  public static StagesSetupStepParameters getStepParameters(StagesConfig stagesConfig, Set<String> childrenNodeIds) {
    if (stagesConfig == null) {
      return StagesSetupStepParameters.newBuilder().childrenNodeIds(childrenNodeIds).build();
    }
    return new StagesSetupStepParameters(stagesConfig.getStages(), childrenNodeIds);
  }
}
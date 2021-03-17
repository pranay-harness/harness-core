package io.harness.steps.approval.stage;

import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.variables.NGVariable;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("approvalStageStepParameters")
public class ApprovalStageStepParameters implements StepParameters {
  String identifier;
  String name;
  ParameterField<String> description;
  List<FailureStrategyConfig> failureStrategies;
  String type;
  List<NGVariable> originalVariables;
  ParameterField<String> skipCondition;

  String childNodeID;

  public static ApprovalStageStepParameters getStepParameters(StageElementConfig config, String childNodeID) {
    if (config == null) {
      return ApprovalStageStepParameters.builder().childNodeID(childNodeID).build();
    }

    return ApprovalStageStepParameters.builder()
        .identifier(config.getIdentifier())
        .name(config.getName())
        .description(config.getDescription())
        .failureStrategies(config.getFailureStrategies())
        .type(config.getType())
        .skipCondition(config.getSkipCondition())
        .originalVariables(config.getVariables())
        .childNodeID(childNodeID)
        .build();
  }
}

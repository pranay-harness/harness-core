package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.variables.NGVariable;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("stageElementParameters")
@OwnedBy(CDC)
public class StageElementParameters implements StepParameters {
  String uuid;
  String identifier;
  String name;
  ParameterField<String> description;

  ParameterField<String> skipCondition;
  ParameterField<String> when;

  List<FailureStrategyConfig> failureStrategies;
  List<NGVariable> originalVariables;
  String type;
  SpecParameters spec;

  @Override
  public String toViewJson() {
    StageElementParameters stageElementParameters = cloneParameters();
    stageElementParameters.setSpec(spec.getViewJsonObject());
    return RecastOrchestrationUtils.toDocumentJson(stageElementParameters);
  }

  public StageElementParameters cloneParameters() {
    return StageElementParameters.builder()
        .uuid(this.uuid)
        .type(this.type)
        .name(this.name)
        .description(this.description)
        .identifier(this.identifier)
        .failureStrategies(this.failureStrategies)
        .when(this.when)
        .skipCondition(this.skipCondition)
        .originalVariables(this.originalVariables)
        .build();
  }
}

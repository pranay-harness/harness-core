package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("stepElementParameters")
@OwnedBy(CDC)
public class StepElementParameters implements StepParameters {
  String uuid;
  String identifier;
  String name;
  String description;
  ParameterField<String> timeout;
  List<FailureStrategyConfig> failureStrategies;

  ParameterField<String> skipCondition;
  ParameterField<String> when;

  String type;
  StepSpecParameters spec;

  ParameterField<List<String>> delegateSelectors;

  @Override
  public String toViewJson() {
    StepElementParameters stepElementParameters = cloneParameters();
    stepElementParameters.setSpec(spec.getViewJsonObject());
    return RecastOrchestrationUtils.toDocumentJson(stepElementParameters);
  }

  public StepElementParameters cloneParameters() {
    return StepElementParameters.builder()
        .uuid(this.uuid)
        .type(this.type)
        .name(this.name)
        .description(this.description)
        .build();
  }
}

package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.when.beans.StepWhenCondition;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("stepElementParameters")
public class StepElementParameters implements StepParameters {
  String uuid;
  String identifier;
  String name;
  String description;
  ParameterField<String> timeout;
  List<FailureStrategyConfig> failureStrategies;

  ParameterField<String> skipCondition;
  StepWhenCondition when;

  String type;
  SpecParameters spec;

  ParameterField<List<String>> delegateSelectors;

  // Only for rollback failures
  OnFailRollbackParameters rollbackParameters;

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
        .identifier(this.identifier)
        .timeout(this.timeout)
        .failureStrategies(this.failureStrategies)
        .when(this.when)
        .skipCondition(this.skipCondition)
        .delegateSelectors(this.delegateSelectors)
        .build();
  }
}

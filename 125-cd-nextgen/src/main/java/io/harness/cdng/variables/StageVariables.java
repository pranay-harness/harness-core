package io.harness.cdng.variables;

import io.harness.yaml.core.PreviousStageAware;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class StageVariables implements PreviousStageAware {
  @Singular private List<Variable> variables;
  private String previousStageIdentifier;
  @Singular private List<Variable> overrides;
}

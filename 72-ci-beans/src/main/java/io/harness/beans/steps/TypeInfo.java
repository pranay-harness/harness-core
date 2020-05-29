package io.harness.beans.steps;

import io.harness.state.StepType;
import io.harness.yaml.core.nonyaml.NonYamlInfo;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class TypeInfo implements NonYamlInfo {
  @NotNull StepInfoType stepInfoType;
  @NotNull StepType stepType;
}

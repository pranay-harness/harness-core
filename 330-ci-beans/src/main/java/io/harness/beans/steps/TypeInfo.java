package io.harness.beans.steps;

import io.harness.pms.steps.StepType;
import io.harness.yaml.core.nonyaml.NonYamlInfo;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

/**
 * Each stepInfo will bind to some step
 * Reason for binding While using execution framework we have to give step type from stepInfo beans
 */
@Value
@Builder
public class TypeInfo implements NonYamlInfo {
  @NotNull CIStepInfoType stepInfoType;
  @NotNull StepType stepType;
}

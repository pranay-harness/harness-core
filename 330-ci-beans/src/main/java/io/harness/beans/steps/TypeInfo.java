package io.harness.beans.steps;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepType;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

/**
 * Each stepInfo will bind to some step
 * Reason for binding While using execution framework we have to give step type from stepInfo beans
 */
@Value
@Builder
@TypeAlias("typeInfo")
@OwnedBy(CI)
public class TypeInfo implements NonYamlInfo {
  @NotNull CIStepInfoType stepInfoType;
  @NotNull StepType stepType;
}

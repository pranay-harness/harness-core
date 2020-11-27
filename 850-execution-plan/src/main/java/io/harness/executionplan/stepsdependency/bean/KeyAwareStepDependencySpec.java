package io.harness.executionplan.stepsdependency.bean;

import io.harness.executionplan.stepsdependency.KeyAware;
import io.harness.executionplan.stepsdependency.StepDependencySpec;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("keyAwareStepDependencySpec")
public class KeyAwareStepDependencySpec implements StepDependencySpec, KeyAware {
  @NonNull String key;
}

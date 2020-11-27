package io.harness.executionplan.stepsdependency;

import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec;
import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec.KeyAwareStepDependencySpecBuilder;

public interface StepDependencySpec {
  static KeyAwareStepDependencySpecBuilder defaultBuilder() {
    return KeyAwareStepDependencySpec.builder();
  }
}

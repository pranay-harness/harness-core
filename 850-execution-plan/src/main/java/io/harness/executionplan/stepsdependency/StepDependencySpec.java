package io.harness.executionplan.stepsdependency;

import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec;
import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec.KeyAwareStepDependencySpecBuilder;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = KeyAwareStepDependencySpec.class)
@JsonSubTypes({ @JsonSubTypes.Type(value = KeyAwareStepDependencySpec.class, name = "keyAware") })
public interface StepDependencySpec {
  static KeyAwareStepDependencySpecBuilder defaultBuilder() {
    return KeyAwareStepDependencySpec.builder();
  }
}

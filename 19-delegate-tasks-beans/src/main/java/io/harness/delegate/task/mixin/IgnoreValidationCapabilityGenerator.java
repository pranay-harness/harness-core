package io.harness.delegate.task.mixin;

import io.harness.delegate.beans.executioncapability.IgnoreValidationCapability;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class IgnoreValidationCapabilityGenerator {
  public static IgnoreValidationCapability buildIgnoreValidationCapability() {
    return IgnoreValidationCapability.builder().build();
  }
}

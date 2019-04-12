package io.harness.delegate.task.mixin;

import io.harness.delegate.beans.executioncapability.IgnoreValidationCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IgnoreValidationCapabilityGenerator {
  private static final Logger logger = LoggerFactory.getLogger(IgnoreValidationCapabilityGenerator.class);

  public static IgnoreValidationCapability buildIgnoreValidationCapability() {
    return IgnoreValidationCapability.builder().build();
  }
}

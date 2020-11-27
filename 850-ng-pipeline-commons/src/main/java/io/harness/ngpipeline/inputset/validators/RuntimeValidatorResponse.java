package io.harness.ngpipeline.inputset.validators;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RuntimeValidatorResponse {
  boolean isValid;
  // This is set only if not valid;
  String errorMessage;
}

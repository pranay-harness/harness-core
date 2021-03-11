package io.harness.accesscontrol.common.validation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ValidationResult {
  boolean valid;
  String errorMessage;
}

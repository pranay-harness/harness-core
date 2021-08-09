package io.harness.pms.yaml.validation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RuntimeValidatorResponse {
  boolean isValid;
  // This is set only if not valid;
  String errorMessage;
}

package io.harness.accesscontrol.common.validation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class ValidationResult {
  public static final ValidationResult VALID = ValidationResult.builder().valid(true).build();
  boolean valid;
  String errorMessage;
}

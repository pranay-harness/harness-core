package io.harness.accesscontrol.common.validation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class ValidationResult {
  boolean valid;
  String errorMessage;
}

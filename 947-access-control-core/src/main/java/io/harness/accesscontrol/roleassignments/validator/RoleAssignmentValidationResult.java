package io.harness.accesscontrol.roleassignments.validator;

import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class RoleAssignmentValidationResult {
  ValidationResult scopeValidationResult;
  ValidationResult principalValidationResult;
  ValidationResult roleValidationResult;
  ValidationResult resourceGroupValidationResult;
}

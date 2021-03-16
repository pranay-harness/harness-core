package io.harness.accesscontrol.roleassignments.validator;

import io.harness.accesscontrol.common.validation.ValidationResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoleAssignmentValidationResult {
  ValidationResult principalValidationResult;
  ValidationResult roleValidationResult;
  ValidationResult resourceGroupValidationResult;
}

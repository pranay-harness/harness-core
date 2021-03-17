package io.harness.accesscontrol.roleassignments.validator;

public interface RoleAssignmentValidator {
  RoleAssignmentValidationResult validate(RoleAssignmentValidationRequest validationRequest);
}

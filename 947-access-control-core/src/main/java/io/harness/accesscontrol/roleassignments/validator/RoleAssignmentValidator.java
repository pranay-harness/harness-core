package io.harness.accesscontrol.roleassignments.validator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public interface RoleAssignmentValidator {
  RoleAssignmentValidationResult validate(RoleAssignmentValidationRequest validationRequest);
}

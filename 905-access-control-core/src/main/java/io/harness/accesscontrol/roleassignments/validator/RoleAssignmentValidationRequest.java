package io.harness.accesscontrol.roleassignments.validator;

import io.harness.accesscontrol.roleassignments.RoleAssignment;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoleAssignmentValidationRequest {
  @NotNull @Valid RoleAssignment roleAssignment;
  @Builder.Default boolean validatePrincipal = true;
  @Builder.Default boolean validateRole = true;
  @Builder.Default boolean validateResourceGroup = true;
}

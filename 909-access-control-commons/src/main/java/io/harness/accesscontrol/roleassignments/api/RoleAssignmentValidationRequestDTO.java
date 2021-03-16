package io.harness.accesscontrol.roleassignments.api;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoleAssignmentValidationRequestDTO {
  @NotNull @Valid RoleAssignmentDTO roleAssignment;
  boolean validatePrincipal;
  boolean validateRole;
  boolean validateResourceGroup;
}

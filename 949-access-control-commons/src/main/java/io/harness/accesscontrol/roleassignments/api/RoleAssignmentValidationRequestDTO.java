package io.harness.accesscontrol.roleassignments.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class RoleAssignmentValidationRequestDTO {
  @NotNull @Valid RoleAssignmentDTO roleAssignment;
  boolean validatePrincipal;
  boolean validateRole;
  boolean validateResourceGroup;
}

package io.harness.accesscontrol.roleassignments;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class RoleAssignmentUpdateResult {
  @NotNull RoleAssignment updatedRoleAssignment;
  @NotNull RoleAssignment originalRoleAssignment;
}

package io.harness.aggregator.consumers;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface RoleAssignmentCRUDEventHandler {
  void handleRoleAssignmentCreate(@NotNull @Valid RoleAssignmentDBO roleAssignment);
  void handleRoleAssignmentDelete(@NotEmpty String roleAssignmentId);
}
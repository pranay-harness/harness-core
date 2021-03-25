package io.harness.ng.accesscontrol.migrations.models;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleAssignmentMetadata {
  private List<RoleAssignmentResponseDTO> createdRoleAssignments;
  private List<RoleAssignmentDTO> failedRoleAssignments;
}

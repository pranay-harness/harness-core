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
  private List<RoleAssignmentDTO> roleAssignmentRequests;
  private AccessControlMigrationStatus status;

  public static AccessControlMigrationStatus getStatus(
      List<RoleAssignmentResponseDTO> createdRoleAssignments, List<RoleAssignmentDTO> roleAssignmentRequests) {
    boolean successful = createdRoleAssignments != null && roleAssignmentRequests != null
        && roleAssignmentRequests.size() == createdRoleAssignments.size();
    if (successful) {
      return AccessControlMigrationStatus.SUCCESS;
    }
    return AccessControlMigrationStatus.PARTIAL_SUCCESS;
  }
}

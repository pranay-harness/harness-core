package io.harness.ng.accesscontrol.mockserver;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;

@OwnedBy(PL)
public interface MockRoleAssignmentService {
  List<RoleAssignmentResponseDTO> createMulti(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      List<RoleAssignmentDTO> filteredRoleAssignments, Boolean managed);

  PageResponse<RoleAssignmentResponseDTO> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      RoleAssignmentFilterDTO roleAssignmentFilter, PageRequest pageRequest);

  RoleAssignmentResponseDTO create(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, RoleAssignmentDTO roleAssignmentDTO);

  void deleteAll(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}

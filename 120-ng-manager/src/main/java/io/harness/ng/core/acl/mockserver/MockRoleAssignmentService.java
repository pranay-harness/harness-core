package io.harness.ng.core.acl.mockserver;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;

public interface MockRoleAssignmentService {
  List<RoleAssignmentResponseDTO> createMulti(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      List<RoleAssignmentDTO> filteredRoleAssignments);

  PageResponse<RoleAssignmentResponseDTO> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      RoleAssignmentFilterDTO roleAssignmentFilter, PageRequest pageRequest);

  RoleAssignmentResponseDTO create(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, RoleAssignmentDTO roleAssignmentDTO);
}

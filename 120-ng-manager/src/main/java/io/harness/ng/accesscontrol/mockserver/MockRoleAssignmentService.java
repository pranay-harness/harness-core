/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.accesscontrol.mockserver;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface MockRoleAssignmentService {
  List<RoleAssignmentResponseDTO> createMulti(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      List<RoleAssignmentDTO> filteredRoleAssignments, Boolean managed);

  PageResponse<RoleAssignmentResponseDTO> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      RoleAssignmentFilterDTO roleAssignmentFilter, PageRequest pageRequest);

  Page<RoleAssignmentResponseDTO> list(Criteria criteria, Pageable pageable);

  RoleAssignmentResponseDTO create(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, RoleAssignmentDTO roleAssignmentDTO);

  void deleteAll(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}

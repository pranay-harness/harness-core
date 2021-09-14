/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.accesscontrol.mockserver;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO.RoleAssignmentDTOKey;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.accesscontrol.mockserver.MockRoleAssignment.MockRoleAssignmentKeys;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.repositories.mockroleassignmentserver.spring.MockRoleAssignmentRepository;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@OwnedBy(PL)
public class MockRoleAssignmentServiceImpl implements MockRoleAssignmentService {
  MockRoleAssignmentRepository mockRoleAssignmentRepository;

  @Inject
  public MockRoleAssignmentServiceImpl(MockRoleAssignmentRepository mockRoleAssignmentRepository) {
    this.mockRoleAssignmentRepository = mockRoleAssignmentRepository;
  }

  @Override
  public List<RoleAssignmentResponseDTO> createMulti(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, List<RoleAssignmentDTO> roleAssignments, Boolean managed) {
    List<RoleAssignmentResponseDTO> createdRoleAssignmentDTOs = new ArrayList<>();
    for (RoleAssignmentDTO roleAssignment : roleAssignments) {
      try {
        roleAssignment = roleAssignment.toBuilder().managed(managed).build();
        RoleAssignmentResponseDTO roleAssignmentResponseDTO =
            create(accountIdentifier, orgIdentifier, projectIdentifier, roleAssignment);
        createdRoleAssignmentDTOs.add(roleAssignmentResponseDTO);
      } catch (Exception e) {
        log.error("Couldn't create roleassignment", e);
      }
    }
    return createdRoleAssignmentDTOs;
  }

  @Override
  public PageResponse<RoleAssignmentResponseDTO> list(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, RoleAssignmentFilterDTO roleAssignmentFilter, PageRequest pageRequest) {
    if (isNotEmpty(roleAssignmentFilter.getResourceGroupFilter())
        || isNotEmpty(roleAssignmentFilter.getDisabledFilter())
        || isNotEmpty(roleAssignmentFilter.getHarnessManagedFilter())
        || isNotEmpty(roleAssignmentFilter.getPrincipalFilter())
        || isNotEmpty(roleAssignmentFilter.getPrincipalTypeFilter())) {
      log.error("Only roleIdentifier filter is supported by mock roleassignment server");
      throw new InvalidRequestException("Only roleIdentifier filter is supported by mock roleassignment server");
    }
    Criteria criteria = Criteria.where(MockRoleAssignmentKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(MockRoleAssignmentKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(MockRoleAssignmentKeys.projectIdentifier)
                            .is(projectIdentifier);
    if (isNotEmpty(roleAssignmentFilter.getRoleFilter())) {
      criteria.and(MockRoleAssignmentKeys.roleAssignment + "." + RoleAssignmentDTOKey.roleIdentifier)
          .in(roleAssignmentFilter.getRoleFilter());
    }
    Pageable page = PageUtils.getPageRequest(pageRequest);
    Page<MockRoleAssignment> pageResponse = mockRoleAssignmentRepository.findAll(criteria, page);
    List<RoleAssignmentResponseDTO> mockRoleAssignments =
        pageResponse.getContent()
            .stream()
            .map(mockRoleAssignment
                -> RoleAssignmentResponseDTO.builder().roleAssignment(mockRoleAssignment.getRoleAssignment()).build())
            .collect(Collectors.toList());
    return PageUtils.getNGPageResponse(pageResponse, mockRoleAssignments);
  }

  @Override
  public Page<RoleAssignmentResponseDTO> list(Criteria criteria, Pageable pageable) {
    return mockRoleAssignmentRepository.findAll(criteria, pageable)
        .map(mockRoleAssignment
            -> RoleAssignmentResponseDTO.builder().roleAssignment(mockRoleAssignment.getRoleAssignment()).build());
  }

  @Override
  public RoleAssignmentResponseDTO create(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, RoleAssignmentDTO roleAssignmentDTO) {
    MockRoleAssignment mockRoleAssignment = MockRoleAssignment.builder()
                                                .accountIdentifier(accountIdentifier)
                                                .orgIdentifier(orgIdentifier)
                                                .projectIdentifier(projectIdentifier)
                                                .roleAssignment(roleAssignmentDTO)
                                                .build();
    try {
      mockRoleAssignment = mockRoleAssignmentRepository.save(mockRoleAssignment);
    } catch (Exception duplicateException) {
      log.error("Can't create roleassignment");
      throw new InvalidRequestException("Can't create roleassignment");
    }
    return RoleAssignmentResponseDTO.builder().roleAssignment(mockRoleAssignment.getRoleAssignment()).build();
  }

  @Override
  public void deleteAll(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    mockRoleAssignmentRepository.deleteAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier);
  }
}

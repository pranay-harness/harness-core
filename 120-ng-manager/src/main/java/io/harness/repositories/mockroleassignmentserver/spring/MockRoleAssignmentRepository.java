/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.mockroleassignmentserver.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.mockserver.MockRoleAssignment;
import io.harness.repositories.mockroleassignmentserver.custom.MockRoleAssignmentRepositoryCustom;

import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface MockRoleAssignmentRepository
    extends PagingAndSortingRepository<MockRoleAssignment, String>, MockRoleAssignmentRepositoryCustom {
  void deleteAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
}

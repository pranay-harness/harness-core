package io.harness.repositories.ng.core.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.acl.mockserver.MockRoleAssignment;
import io.harness.repositories.ng.core.custom.MockRoleAssignmentRepositoryCustom;

import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface MockRoleAssignmentRepository
    extends PagingAndSortingRepository<MockRoleAssignment, String>, MockRoleAssignmentRepositoryCustom {}

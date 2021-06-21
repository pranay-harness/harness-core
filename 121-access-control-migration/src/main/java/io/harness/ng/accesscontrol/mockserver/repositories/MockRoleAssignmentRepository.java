package io.harness.ng.accesscontrol.mockserver.repositories;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.mockserver.models.MockRoleAssignment;

import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface MockRoleAssignmentRepository
    extends PagingAndSortingRepository<MockRoleAssignment, String>, MockRoleAssignmentRepositoryCustom {
  void deleteAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
}

package io.harness.accesscontrol.roleassignments.database.repositories;

import io.harness.accesscontrol.roleassignments.database.RoleAssignment;
import io.harness.annotation.HarnessRepo;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface RoleAssignmentRepository
    extends PagingAndSortingRepository<RoleAssignment, String>, RoleAssignmentCustomRepository {
  Optional<RoleAssignment> findByIdentifierAndParentIdentifier(String identifier, String parentIdentifier);
  RoleAssignment deleteByIdentifierAndParentIdentifier(String identifier, String parentIdentifier);
}
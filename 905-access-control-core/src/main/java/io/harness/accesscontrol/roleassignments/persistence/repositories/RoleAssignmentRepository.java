package io.harness.accesscontrol.roleassignments.persistence.repositories;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.annotation.HarnessRepo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface RoleAssignmentRepository
    extends PagingAndSortingRepository<RoleAssignmentDBO, String>, RoleAssignmentCustomRepository {
  Optional<RoleAssignmentDBO> findByIdentifierAndScopeIdentifier(String identifier, String parentIdentifier);

  List<RoleAssignmentDBO> deleteByIdentifierAndScopeIdentifier(String identifier, String parentIdentifier);

  List<RoleAssignmentDBO> findByPrincipalIdentifierAndPrincipalType(String principal, PrincipalType principalType);
}
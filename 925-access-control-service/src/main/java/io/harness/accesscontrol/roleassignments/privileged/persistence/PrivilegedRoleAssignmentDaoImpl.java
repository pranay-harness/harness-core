/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roleassignments.privileged.persistence;

import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.roleassignments.privileged.PrivilegedRoleAssignment;
import io.harness.accesscontrol.roleassignments.privileged.persistence.PrivilegedRoleAssignmentDBO.PrivilegedRoleAssignmentDBOKeys;
import io.harness.accesscontrol.roleassignments.privileged.persistence.repositories.PrivilegedRoleAssignmentRepository;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PL)
@Singleton
@ValidateOnExecution
public class PrivilegedRoleAssignmentDaoImpl implements PrivilegedRoleAssignmentDao {
  private final PrivilegedRoleAssignmentRepository repository;

  @Inject
  public PrivilegedRoleAssignmentDaoImpl(PrivilegedRoleAssignmentRepository repository) {
    this.repository = repository;
  }

  @Override
  public long insertAllIgnoringDuplicates(List<PrivilegedRoleAssignment> privilegedRoleAssignments) {
    List<PrivilegedRoleAssignmentDBO> assignments =
        privilegedRoleAssignments.stream().map(PrivilegedRoleAssignmentDBOMapper::toDBO).collect(Collectors.toList());
    return repository.insertAllIgnoringDuplicates(assignments);
  }

  @Override
  public List<PrivilegedRoleAssignment> getByPrincipal(
      Principal principal, Set<String> scopes, ManagedFilter managedFilter) {
    Criteria scopeCriteria = new Criteria();
    scopeCriteria.orOperator(Criteria.where(PrivilegedRoleAssignmentDBOKeys.scopeIdentifier).in(scopes),
        Criteria.where(PrivilegedRoleAssignmentDBOKeys.global).is(true));
    Criteria userIdentifierCriteria = Criteria.where(PrivilegedRoleAssignmentDBOKeys.principalIdentifier)
                                          .is(principal.getPrincipalIdentifier())
                                          .and(PrivilegedRoleAssignmentDBOKeys.principalType)
                                          .is(principal.getPrincipalType());
    Criteria finalCriteria = getManagedCriteria(managedFilter).andOperator(userIdentifierCriteria, scopeCriteria);

    List<PrivilegedRoleAssignmentDBO> assignments = repository.get(finalCriteria);
    return assignments.stream().map(PrivilegedRoleAssignmentDBOMapper::fromDBO).collect(Collectors.toList());
  }

  @Override
  public List<PrivilegedRoleAssignment> getGlobalByRole(String roleIdentifier, ManagedFilter managedFilter) {
    Criteria criteria = getManagedCriteria(managedFilter)
                            .and(PrivilegedRoleAssignmentDBOKeys.global)
                            .is(true)
                            .and(PrivilegedRoleAssignmentDBOKeys.roleIdentifier)
                            .is(roleIdentifier);
    List<PrivilegedRoleAssignmentDBO> assignments = repository.get(criteria);
    return assignments.stream().map(PrivilegedRoleAssignmentDBOMapper::fromDBO).collect(Collectors.toList());
  }

  @Override
  public long removeGlobalByPrincipalsAndRole(
      Set<Principal> principals, String roleIdentifier, ManagedFilter managedFilter) {
    Criteria criteria = getManagedCriteria(managedFilter)
                            .and(PrivilegedRoleAssignmentDBOKeys.global)
                            .is(true)
                            .and(PrivilegedRoleAssignmentDBOKeys.roleIdentifier)
                            .is(roleIdentifier);
    criteria.orOperator(principals.stream()
                            .map(principal
                                -> Criteria.where(PrivilegedRoleAssignmentDBOKeys.principalIdentifier)
                                       .is(principal.getPrincipalIdentifier())
                                       .and(PrivilegedRoleAssignmentDBOKeys.principalType)
                                       .is(principal.getPrincipalType()))
                            .toArray(Criteria[] ::new));
    return repository.remove(criteria);
  }

  @Override
  public long deleteByRoleAssignment(String id, ManagedFilter managedFilter) {
    Criteria criteria =
        getManagedCriteria(managedFilter).and(PrivilegedRoleAssignmentDBOKeys.linkedRoleAssignment).is(id);
    return repository.remove(criteria);
  }

  @Override
  public long deleteByUserGroup(String identifier, String scopeIdentifier, ManagedFilter managedFilter) {
    Criteria criteria = getManagedCriteria(managedFilter)
                            .and(PrivilegedRoleAssignmentDBOKeys.userGroupIdentifier)
                            .is(identifier)
                            .and(PrivilegedRoleAssignmentDBOKeys.scopeIdentifier)
                            .is(scopeIdentifier);
    return repository.remove(criteria);
  }

  private Criteria getManagedCriteria(ManagedFilter managedFilter) {
    if (ManagedFilter.ONLY_MANAGED.equals(managedFilter)) {
      return Criteria.where(PrivilegedRoleAssignmentDBOKeys.managed).is(true);
    }
    if (ManagedFilter.ONLY_CUSTOM.equals(managedFilter)) {
      return Criteria.where(PrivilegedRoleAssignmentDBOKeys.managed).is(false);
    }
    return new Criteria();
  }
}

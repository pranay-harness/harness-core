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
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface PrivilegedRoleAssignmentDao {
  long insertAllIgnoringDuplicates(List<PrivilegedRoleAssignment> privilegedRoleAssignments);
  List<PrivilegedRoleAssignment> getByPrincipal(
      @NotNull Principal principal, @NotEmpty Set<String> scopes, @NotNull ManagedFilter managedFilter);
  List<PrivilegedRoleAssignment> getGlobalByRole(@NotEmpty String roleIdentifier, @NotNull ManagedFilter managedFilter);
  long removeGlobalByPrincipalsAndRole(
      @NotEmpty Set<Principal> principals, @NotEmpty String roleIdentifier, @NotNull ManagedFilter managedFilter);
  long deleteByRoleAssignment(@NotEmpty String id, @NotNull ManagedFilter managedFilter);
  long deleteByUserGroup(String identifier, String scopeIdentifier, @NotNull ManagedFilter managedFilter);
}

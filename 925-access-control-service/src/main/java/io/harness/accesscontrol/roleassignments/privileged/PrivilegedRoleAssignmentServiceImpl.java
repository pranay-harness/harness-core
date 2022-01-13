/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roleassignments.privileged;

import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;
import static io.harness.accesscontrol.common.filter.ManagedFilter.ONLY_CUSTOM;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.accesscontrol.acl.PermissionCheck;
import io.harness.accesscontrol.acl.PermissionCheckResult;
import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.roleassignments.privileged.persistence.PrivilegedRoleAssignmentDao;
import io.harness.accesscontrol.roles.PrivilegedRole;
import io.harness.accesscontrol.roles.PrivilegedRolesConfig;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.harness.HarnessScopeLevel;
import io.harness.accesscontrol.support.SupportPreference;
import io.harness.accesscontrol.support.SupportService;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@OwnedBy(HarnessTeam.PL)
@Singleton
@ValidateOnExecution
public class PrivilegedRoleAssignmentServiceImpl implements PrivilegedRoleAssignmentService {
  private final PrivilegedRoleAssignmentDao dao;
  private final Set<PrivilegedRole> privilegedRoles;
  private final SupportService supportService;
  private final Map<String, ScopeLevel> scopeLevelsByResourceType;
  private final String PRIVILEGED_ROLES_CONFIG_PATH = "io/harness/accesscontrol/roles/privileged-roles.yml";

  @Inject
  public PrivilegedRoleAssignmentServiceImpl(
      PrivilegedRoleAssignmentDao dao, SupportService supportService, Map<String, ScopeLevel> scopeLevels) {
    this.dao = dao;
    this.supportService = supportService;
    this.scopeLevelsByResourceType = new HashMap<>();
    scopeLevels.values().forEach(scopeLevel -> scopeLevelsByResourceType.put(scopeLevel.getResourceType(), scopeLevel));
    this.privilegedRoles = getPrivilegedRoles();
  }

  private Set<PrivilegedRole> getPrivilegedRoles() {
    ObjectMapper om = new ObjectMapper(new YAMLFactory());
    URL url = getClass().getClassLoader().getResource(PRIVILEGED_ROLES_CONFIG_PATH);
    try {
      byte[] bytes = Resources.toByteArray(url);
      return om.readValue(bytes, PrivilegedRolesConfig.class).getRoles();
    } catch (IOException e) {
      throw new InvalidRequestException("Super Roles file path or format is invalid");
    }
  }

  @Override
  public void saveAll(Set<PrivilegedRoleAssignment> privilegedRoleAssignments) {
    dao.insertAllIgnoringDuplicates(new ArrayList<>(privilegedRoleAssignments));
  }

  @Override
  public PrivilegedAccessResult checkAccess(PrivilegedAccessCheck privilegedAccessCheck) {
    List<PermissionCheckResult> permissionCheckResults =
        privilegedAccessCheck.getPermissionChecks()
            .stream()
            .map(permissionCheck
                -> checkAccess(privilegedAccessCheck.getAccountIdentifier(), privilegedAccessCheck.getPrincipal(),
                    permissionCheck))
            .collect(Collectors.toList());

    return PrivilegedAccessResult.builder()
        .accountIdentifier(privilegedAccessCheck.getAccountIdentifier())
        .principal(privilegedAccessCheck.getPrincipal())
        .permissionCheckResults(permissionCheckResults)
        .build();
  }

  private PermissionCheckResult checkAccess(
      String accountIdentifier, Principal principal, PermissionCheck permissionCheck) {
    SupportPreference supportPreference = supportService.fetchSupportPreference(accountIdentifier);
    ManagedFilter managedFilter = supportPreference.isSupportEnabled() ? NO_FILTER : ONLY_CUSTOM;
    Scope resourceScope = permissionCheck.getResourceScope() == null
        ? Scope.builder().level(HarnessScopeLevel.ACCOUNT).instanceId(accountIdentifier).build()
        : permissionCheck.getResourceScope();
    if (scopeLevelsByResourceType.containsKey(permissionCheck.getResourceType())
        && !resourceScope.getLevel().getResourceType().equals(permissionCheck.getResourceType())
        && isNotEmpty(permissionCheck.getResourceIdentifier())) {
      resourceScope = Scope.builder()
                          .level(scopeLevelsByResourceType.get(permissionCheck.getResourceType()))
                          .parentScope(resourceScope)
                          .instanceId(permissionCheck.getResourceIdentifier())
                          .build();
    }
    Set<Scope> privilegedRoleAssignmentScopes = new HashSet<>();
    while (resourceScope != null) {
      privilegedRoleAssignmentScopes.add(resourceScope);
      resourceScope = resourceScope.getParentScope();
    }
    Set<String> allAllowedPermissions = getAllAllowedPermissions(principal,
        privilegedRoleAssignmentScopes.stream().map(Scope::toString).collect(Collectors.toSet()), managedFilter);
    return PermissionCheckResult.builder()
        .resourceScope(permissionCheck.getResourceScope())
        .resourceType(permissionCheck.getResourceType())
        .resourceIdentifier(permissionCheck.getResourceIdentifier())
        .permission(permissionCheck.getPermission())
        .permitted(allAllowedPermissions.contains(permissionCheck.getPermission()))
        .build();
  }

  private Set<String> getAllAllowedPermissions(Principal principal, Set<String> scopes, ManagedFilter managedFilter) {
    List<PrivilegedRoleAssignment> privilegedRoleAssignments = dao.getByPrincipal(principal, scopes, managedFilter);
    Set<String> privilegedRoleIdentifiers =
        privilegedRoleAssignments.stream().map(PrivilegedRoleAssignment::getRoleIdentifier).collect(Collectors.toSet());
    return privilegedRoles.stream()
        .filter(privilegedRole -> privilegedRoleIdentifiers.contains(privilegedRole.getIdentifier()))
        .map(PrivilegedRole::getPermissions)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public void syncManagedGlobalRoleAssignments(Set<Principal> updatedPrincipals, String roleIdentifier) {
    List<PrivilegedRoleAssignment> privilegedRoleAssignments =
        dao.getGlobalByRole(roleIdentifier, ManagedFilter.ONLY_MANAGED);
    Set<Principal> savedPrincipals = privilegedRoleAssignments.stream()
                                         .map(r
                                             -> Principal.builder()
                                                    .principalType(r.getPrincipalType())
                                                    .principalIdentifier(r.getPrincipalIdentifier())
                                                    .build())
                                         .collect(Collectors.toSet());

    Set<Principal> removedPrincipals = Sets.difference(savedPrincipals, updatedPrincipals);
    Set<Principal> addedPrincipals = Sets.difference(updatedPrincipals, savedPrincipals);

    if (!removedPrincipals.isEmpty()) {
      dao.removeGlobalByPrincipalsAndRole(removedPrincipals, roleIdentifier, ManagedFilter.ONLY_MANAGED);
    }

    if (!addedPrincipals.isEmpty()) {
      List<PrivilegedRoleAssignment> newRoleAssignments =
          updatedPrincipals.stream()
              .map(principal
                  -> PrivilegedRoleAssignment.builder()
                         .principalIdentifier(principal.getPrincipalIdentifier())
                         .principalType(principal.getPrincipalType())
                         .roleIdentifier(roleIdentifier)
                         .global(true)
                         .managed(true)
                         .build())
              .collect(Collectors.toList());
      dao.insertAllIgnoringDuplicates(newRoleAssignments);
    }
  }

  @Override
  public void deleteByRoleAssignment(String id) {
    dao.deleteByRoleAssignment(id, ONLY_CUSTOM);
  }

  @Override
  public void deleteByUserGroup(String identifier, String scopeIdentifier) {
    dao.deleteByUserGroup(identifier, scopeIdentifier, ONLY_CUSTOM);
  }
}

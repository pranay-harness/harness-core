package io.harness.accesscontrol.roleassignments.privileged;

import static io.harness.accesscontrol.acl.api.ACLResourceHelper.getAccessControlDTO;
import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;
import static io.harness.accesscontrol.common.filter.ManagedFilter.ONLY_CUSTOM;

import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.roleassignments.privileged.persistence.PrivilegedRoleAssignmentDao;
import io.harness.accesscontrol.roles.PrivilegedRole;
import io.harness.accesscontrol.roles.PrivilegedRolesConfig;
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
import java.util.HashSet;
import java.util.List;
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
  private final String SUPER_ROLES_CONFIG_FILEPATH = "io/harness/accesscontrol/roles/privileged-roles.yml";

  @Inject
  public PrivilegedRoleAssignmentServiceImpl(PrivilegedRoleAssignmentDao dao, SupportService supportService) {
    this.dao = dao;
    this.supportService = supportService;
    this.privilegedRoles = getPrivilegedRoles();
  }

  private Set<PrivilegedRole> getPrivilegedRoles() {
    ObjectMapper om = new ObjectMapper(new YAMLFactory());
    URL url = getClass().getClassLoader().getResource(SUPER_ROLES_CONFIG_FILEPATH);
    try {
      byte[] bytes = Resources.toByteArray(url);
      return om.readValue(bytes, PrivilegedRolesConfig.class).getRoles();
    } catch (IOException e) {
      throw new InvalidRequestException("Super Roles file path or format is invalid");
    }
  }

  @Override
  public PrivilegedAccessResult checkAccess(PrivilegedAccessCheck privilegedAccessCheck) {
    Set<String> allAllowedPermissions;
    if (!PrincipalType.USER.equals(privilegedAccessCheck.getPrincipal().getPrincipalType())) {
      allAllowedPermissions = new HashSet<>();
    } else {
      allAllowedPermissions =
          getAllAllowedPermissions(privilegedAccessCheck.getPrincipal(), privilegedAccessCheck.getAccountIdentifier());
    }

    List<AccessControlDTO> permissionCheckResults =
        privilegedAccessCheck.getPermissions()
            .stream()
            .map(permissionCheck
                -> getAccessControlDTO(
                    permissionCheck, allAllowedPermissions.contains(permissionCheck.getPermission())))
            .collect(Collectors.toList());

    return PrivilegedAccessResult.builder()
        .accountIdentifier(privilegedAccessCheck.getAccountIdentifier())
        .principal(privilegedAccessCheck.getPrincipal())
        .permissionCheckResults(permissionCheckResults)
        .build();
  }

  private Set<String> getAllAllowedPermissions(Principal principal, String accountIdentifier) {
    SupportPreference supportPreference = supportService.fetchSupportPreference(accountIdentifier);
    ManagedFilter managedFilter = supportPreference.isSupportEnabled() ? NO_FILTER : ONLY_CUSTOM;

    List<PrivilegedRoleAssignment> privilegedRoleAssignments =
        dao.getByPrincipal(principal, accountIdentifier, managedFilter);

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
}

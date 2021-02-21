package io.harness.accesscontrol.roles;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.permissions.PermissionFilter;
import io.harness.accesscontrol.permissions.PermissionService;
import io.harness.accesscontrol.permissions.PermissionStatus;
import io.harness.accesscontrol.roles.persistence.RoleDao;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ValidateOnExecution
public class RoleServiceImpl implements RoleService {
  private final RoleDao roleDao;
  private final PermissionService permissionService;
  private final ScopeService scopeService;

  @Inject
  public RoleServiceImpl(RoleDao roleDao, PermissionService permissionService, ScopeService scopeService) {
    this.roleDao = roleDao;
    this.permissionService = permissionService;
    this.scopeService = scopeService;
  }

  @Override
  public Role create(Role role) {
    validateScopes(role);
    validatePermissions(role);
    return roleDao.create(role);
  }

  @Override
  public PageResponse<Role> list(PageRequest pageRequest, String scopeIdentifier, boolean includeManaged) {
    if (isEmpty(scopeIdentifier) && !includeManaged) {
      throw new InvalidRequestException(
          "Either includeManaged should be true, or parentIdentifier should be non-empty");
    }
    Set<String> allowedScopeLevels = new HashSet<>();
    if (isNotEmpty(scopeIdentifier)) {
      Scope scope = scopeService.buildScopeFromScopeIdentifier(scopeIdentifier);
      allowedScopeLevels.add(scope.getLevel().toString());
    }
    return roleDao.list(pageRequest, scopeIdentifier, includeManaged, allowedScopeLevels);
  }

  @Override
  public Optional<Role> get(String identifier, String scopeIdentifier, boolean isManaged) {
    return roleDao.get(identifier, scopeIdentifier, isManaged);
  }

  @Override
  public Role update(Role roleUpdate) {
    Optional<Role> currentRoleOptional =
        get(roleUpdate.getIdentifier(), roleUpdate.getScopeIdentifier(), roleUpdate.isManaged());
    if (!currentRoleOptional.isPresent()) {
      throw new InvalidRequestException(
          String.format("Could not find the role in the scope %s", roleUpdate.getScopeIdentifier()));
    }
    Role currentRole = currentRoleOptional.get();
    if (!currentRole.getAllowedScopeLevels().equals(roleUpdate.getAllowedScopeLevels())) {
      throw new InvalidRequestException("Cannot change the the scopes at which this role can be used.");
    }
    validatePermissions(roleUpdate);
    roleUpdate.setVersion(currentRole.getVersion());
    roleUpdate.setCreatedAt(currentRole.getCreatedAt());
    roleUpdate.setLastModifiedAt(currentRole.getLastModifiedAt());
    return roleDao.update(roleUpdate);
  }

  @Override
  public boolean removePermissionFromRoles(String permissionIdentifier) {
    return roleDao.removePermissionFromRoles(permissionIdentifier);
  }

  @Override
  public Role delete(String identifier, String scopeIdentifier, boolean isManaged) {
    Optional<Role> currentRoleOptional = get(identifier, scopeIdentifier, isManaged);
    if (!currentRoleOptional.isPresent()) {
      throw new InvalidRequestException(String.format("Could not find the role in the scope %s", scopeIdentifier));
    }
    return roleDao.delete(identifier, scopeIdentifier)
        .orElseThrow(()
                         -> new UnexpectedException(String.format(
                             "Failed to delete the role %s in the scope %s", identifier, scopeIdentifier)));
  }

  private void validatePermissions(Role role) {
    Set<PermissionStatus> allowedPermissionStatus =
        Sets.newHashSet(PermissionStatus.ACTIVE, PermissionStatus.DEPRECATED);
    PermissionFilter permissionFilter = PermissionFilter.builder()
                                            .identifierFilter(role.getPermissions())
                                            .statusFilter(allowedPermissionStatus)
                                            .allowedScopeLevelsFilter(role.getAllowedScopeLevels())
                                            .build();
    List<Permission> permissionList = permissionService.list(permissionFilter);
    permissionList = permissionList == null ? new ArrayList<>() : permissionList;
    Set<String> validPermissions = permissionList.stream().map(Permission::getIdentifier).collect(Collectors.toSet());
    Set<String> invalidPermissions = Sets.difference(role.getPermissions(), validPermissions);
    if (!invalidPermissions.isEmpty()) {
      log.error("Invalid permissions while creating role {} in scope {} : [ {} ]", role.getIdentifier(),
          role.getScopeIdentifier(), String.join(",", invalidPermissions));
      throw new InvalidArgumentsException(
          "Some of the specified permissions in the role are invalid or cannot be given at this scope. Please check the permissions again");
    }
  }

  private void validateScopes(Role role) {
    if (role.isManaged() && !scopeService.areScopeLevelsValid(role.getAllowedScopeLevels())) {
      throw new InvalidArgumentsException(
          String.format("The provided scopes are not registered in the service. Please select scopes out of [ %s ]",
              String.join(",", scopeService.getAllScopeLevels())));
    }
    if (!role.isManaged()) {
      String scopeLevel = scopeService.buildScopeFromScopeIdentifier(role.getScopeIdentifier()).getLevel().toString();
      if (role.getAllowedScopeLevels().size() > 1 || !role.getAllowedScopeLevels().contains(scopeLevel)) {
        throw new InvalidArgumentsException(String.format(
            "This custom role can be only used at '%s' level. Please set the allowedScopeLevels to contain only the %s level.",
            scopeLevel, scopeLevel));
      }
    }
  }
}

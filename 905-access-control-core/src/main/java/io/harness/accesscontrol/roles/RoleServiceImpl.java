package io.harness.accesscontrol.roles;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.permissions.PermissionFilter;
import io.harness.accesscontrol.permissions.PermissionService;
import io.harness.accesscontrol.permissions.PermissionStatus;
import io.harness.accesscontrol.roles.persistence.RoleDao;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    if (role.isManaged() && isNotEmpty(role.getScopeIdentifier())) {
      throw new InvalidArgumentsException("A managed role cannot be created in a scope.");
    }
    validateScopes(role);
    validatePermissions(role);
    return roleDao.create(role);
  }

  @Override
  public PageResponse<Role> getAll(PageRequest pageRequest, String parentIdentifier, boolean includeManaged) {
    if (isEmpty(parentIdentifier) && !includeManaged) {
      throw new InvalidRequestException(
          "Either includeManaged should be true, or parentIdentifier should be non-empty");
    }
    return roleDao.getAll(pageRequest, parentIdentifier, includeManaged);
  }

  @Override
  public Optional<Role> get(String identifier, String parentIdentifier) {
    return roleDao.get(identifier, parentIdentifier);
  }

  @Override
  public Role update(Role roleUpdate) {
    Optional<Role> currentRoleOptional = get(roleUpdate.getIdentifier(), roleUpdate.getScopeIdentifier());
    if (!currentRoleOptional.isPresent()) {
      throw new InvalidRequestException(
          String.format("Could not find the role in the scope %s", roleUpdate.getScopeIdentifier()));
    }
    Role currentRole = currentRoleOptional.get();
    if (!currentRole.getAllowedScopeLevels().equals(roleUpdate.getAllowedScopeLevels())) {
      throw new InvalidRequestException("Cannot change the the scopes at which this role can be used.");
    }
    if (currentRole.isManaged() != roleUpdate.isManaged()) {
      throw new InvalidRequestException("Cannot convert a custom role to a managed role.");
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
  public Optional<Role> delete(String identifier, String scopeIdentifier, boolean removeManagedRole) {
    Optional<Role> currentRoleOptional = get(identifier, scopeIdentifier);
    if (!currentRoleOptional.isPresent()) {
      throw new InvalidRequestException(String.format("Could not find the role in the scope %s", scopeIdentifier));
    }
    if (!removeManagedRole && currentRoleOptional.get().isManaged()) {
      throw new InvalidRequestException("Cannot delete a managed role");
    }
    return roleDao.delete(identifier, scopeIdentifier);
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

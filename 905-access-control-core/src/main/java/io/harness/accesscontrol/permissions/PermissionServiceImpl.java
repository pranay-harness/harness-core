package io.harness.accesscontrol.permissions;

import io.harness.accesscontrol.permissions.persistence.PermissionDao;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class PermissionServiceImpl implements PermissionService {
  private final PermissionDao permissionDao;
  private final ScopeService scopeService;

  @Inject
  public PermissionServiceImpl(PermissionDao permissionDao, ScopeService scopeService) {
    this.permissionDao = permissionDao;
    this.scopeService = scopeService;
  }

  @Override
  public Permission create(Permission permission) {
    if (!scopeService.areScopeLevelsValid(permission.getAllowedScopeLevels())) {
      throw new InvalidRequestException(
          String.format("The scopes provided in the permission %s are invalid. Please select scopes from [ %s ]",
              permission.getIdentifier(), String.join(",", scopeService.getAllScopeLevels())));
    }
    return permissionDao.create(permission);
  }

  @Override
  public Optional<Permission> get(String identifier) {
    return permissionDao.get(identifier);
  }

  @Override
  public List<Permission> list(@Valid @NotNull PermissionFilter permissionFilter) {
    return permissionDao.list(permissionFilter);
  }

  @Override
  public Permission update(Permission permissionUpdate) {
    Optional<Permission> currentPermissionOptional = get(permissionUpdate.getIdentifier());
    if (!currentPermissionOptional.isPresent()) {
      throw new InvalidRequestException(
          String.format("Could not find the permission %s", permissionUpdate.getIdentifier()));
    }
    Permission currentPermission = currentPermissionOptional.get();
    if (!permissionUpdate.getAllowedScopeLevels().equals(currentPermission.getAllowedScopeLevels())) {
      throw new InvalidRequestException("Cannot change the the scopes at which this permission can be used.");
    }
    permissionUpdate.setVersion(currentPermission.getVersion());
    return permissionDao.update(permissionUpdate);
  }

  @Override
  public void delete(String identifier) {
    permissionDao.delete(identifier);
  }
}

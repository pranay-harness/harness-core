package io.harness.accesscontrol.permissions;

import io.harness.accesscontrol.permissions.persistence.PermissionDao;
import io.harness.accesscontrol.scopes.Scope;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class PermissionServiceImpl implements PermissionService {
  private final PermissionDao permissionDao;

  @Inject
  public PermissionServiceImpl(PermissionDao permissionDao) {
    this.permissionDao = permissionDao;
  }

  @Override
  public Permission create(Permission permission) {
    return permissionDao.create(permission);
  }

  @Override
  public Optional<Permission> get(String identifier) {
    return permissionDao.get(identifier);
  }

  @Override
  public List<Permission> list(Scope scope, String resourceType) {
    return permissionDao.list(scope, resourceType);
  }

  @Override
  public String update(Permission permission) {
    return permissionDao.update(permission);
  }

  @Override
  public void delete(String identifier) {
    permissionDao.delete(identifier);
  }
}

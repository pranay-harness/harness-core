package io.harness.accesscontrol.permissions;

import io.harness.accesscontrol.permissions.persistence.PermissionDao;

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
  public List<Permission> list(@Valid @NotNull PermissionFilter permissionFilter) {
    return permissionDao.list(permissionFilter);
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

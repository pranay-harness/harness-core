package io.harness.accesscontrol.permissions;

import io.harness.accesscontrol.permissions.database.PermissionDao;
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
  PermissionServiceImpl(PermissionDao permissionDao) {
    this.permissionDao = permissionDao;
  }

  @Override
  public String create(PermissionDTO permissionDTO) {
    return permissionDao.create(permissionDTO);
  }

  @Override
  public Optional<PermissionDTO> get(String identifier) {
    return permissionDao.get(identifier);
  }

  @Override
  public List<PermissionDTO> list(Scope scope, String resourceType) {
    return permissionDao.list(scope, resourceType);
  }

  @Override
  public String update(PermissionDTO permissionDTO) {
    return permissionDao.update(permissionDTO);
  }

  @Override
  public void delete(String identifier) {
    permissionDao.delete(identifier);
  }
}

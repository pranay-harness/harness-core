package io.harness.accesscontrol.permissions;

import io.harness.accesscontrol.permissions.database.PermissionDao;
import io.harness.accesscontrol.permissions.database.PermissionDaoImpl;
import io.harness.accesscontrol.scopes.ScopeService;

import com.google.inject.AbstractModule;

public class PermissionsModule extends AbstractModule {
  private static PermissionsModule instance;

  public static PermissionsModule getInstance() {
    if (instance == null) {
      instance = new PermissionsModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(PermissionService.class).to(PermissionServiceImpl.class);
    bind(PermissionDao.class).to(PermissionDaoImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(ScopeService.class);
  }
}

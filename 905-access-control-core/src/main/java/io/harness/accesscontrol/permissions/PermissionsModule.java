package io.harness.accesscontrol.permissions;

import io.harness.accesscontrol.permissions.database.PermissionDao;
import io.harness.accesscontrol.permissions.database.PermissionPersistenceModule;

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
    install(PermissionPersistenceModule.getInstance());
    bind(PermissionService.class).to(PermissionServiceImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(PermissionDao.class);
  }
}

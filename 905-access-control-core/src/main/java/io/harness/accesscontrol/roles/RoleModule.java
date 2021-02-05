package io.harness.accesscontrol.roles;

import io.harness.accesscontrol.roles.database.RoleDao;
import io.harness.accesscontrol.roles.database.RoleDaoImpl;
import io.harness.accesscontrol.scopes.ScopeService;

import com.google.inject.AbstractModule;

public class RoleModule extends AbstractModule {
  private static RoleModule instance;

  public static RoleModule getInstance() {
    if (instance == null) {
      instance = new RoleModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(RoleService.class).to(RoleServiceImpl.class);
    bind(RoleDao.class).to(RoleDaoImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(ScopeService.class);
    requireBinding(RoleDao.class);
  }
}

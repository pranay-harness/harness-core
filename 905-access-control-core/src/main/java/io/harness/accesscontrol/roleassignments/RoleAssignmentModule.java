package io.harness.accesscontrol.roleassignments;

import io.harness.accesscontrol.roleassignments.database.RoleAssignmentDao;
import io.harness.accesscontrol.roleassignments.database.RoleAssignmentDaoImpl;
import io.harness.accesscontrol.scopes.ScopeService;

import com.google.inject.AbstractModule;

public class RoleAssignmentModule extends AbstractModule {
  private static RoleAssignmentModule instance;

  public static RoleAssignmentModule getInstance() {
    if (instance == null) {
      instance = new RoleAssignmentModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(RoleAssignmentService.class).to(RoleAssignmentServiceImpl.class);
    bind(RoleAssignmentDao.class).to(RoleAssignmentDaoImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(ScopeService.class);
  }
}

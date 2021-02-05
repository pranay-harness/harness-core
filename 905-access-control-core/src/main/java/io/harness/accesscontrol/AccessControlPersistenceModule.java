package io.harness.accesscontrol;

import io.harness.accesscontrol.permissions.database.PermissionPersistenceConfig;
import io.harness.accesscontrol.roles.database.RolePersistenceConfig;
import io.harness.mongo.MongoConfig;
import io.harness.springdata.PersistenceModule;

public class AccessControlPersistenceModule extends PersistenceModule {
  private static AccessControlPersistenceModule instance;

  public static AccessControlPersistenceModule getInstance() {
    if (instance == null) {
      instance = new AccessControlPersistenceModule();
    }
    return instance;
  }

  @Override
  public void configure() {
    super.configure();
    registerRequiredBindings();
  }

  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {PermissionPersistenceConfig.class, RolePersistenceConfig.class};
  }

  private void registerRequiredBindings() {
    requireBinding(MongoConfig.class);
  }
}

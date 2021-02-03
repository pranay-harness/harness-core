package io.harness.accesscontrol.permissions.database;

import io.harness.mongo.MongoConfig;
import io.harness.springdata.PersistenceModule;

public class PermissionPersistenceModule extends PersistenceModule {
  private static PermissionPersistenceModule instance;

  public static PermissionPersistenceModule getInstance() {
    if (instance == null) {
      instance = new PermissionPersistenceModule();
    }
    return instance;
  }

  @Override
  public void configure() {
    super.configure();
    bind(PermissionDao.class).to(PermissionDaoImpl.class);
    registerRequiredBindings();
  }

  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {PermissionPersistenceConfig.class};
  }

  private void registerRequiredBindings() {
    requireBinding(MongoConfig.class);
  }
}

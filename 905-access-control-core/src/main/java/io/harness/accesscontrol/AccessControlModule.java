package io.harness.accesscontrol;

import io.harness.accesscontrol.permissions.PermissionsModule;
import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class AccessControlModule extends AbstractModule {
  private static AccessControlModule instance;
  private final AccessControlConfiguration accessControlConfiguration;

  private AccessControlModule(AccessControlConfiguration accessControlConfiguration) {
    this.accessControlConfiguration = accessControlConfiguration;
  }

  public static AccessControlModule getInstance(AccessControlConfiguration accessControlConfiguration) {
    if (instance == null) {
      instance = new AccessControlModule(accessControlConfiguration);
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return accessControlConfiguration.getMongoConfig();
      }
    });
    install(PermissionsModule.getInstance());
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {}
}

package io.harness.accesscontrol;

import io.harness.accesscontrol.permissions.PermissionsModule;
import io.harness.accesscontrol.roleassignments.RoleAssignmentModule;
import io.harness.accesscontrol.roles.RoleModule;
import io.harness.accesscontrol.scopes.ScopeModule;
import io.harness.mongo.MongoConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import ru.vyarus.guice.validator.ValidationModule;

public class AccessControlModule extends AbstractModule {
  private static AccessControlModule instance;
  private final AccessControlConfiguration accessControlConfiguration;

  private AccessControlModule(AccessControlConfiguration accessControlConfiguration) {
    this.accessControlConfiguration = accessControlConfiguration;
  }

  public static synchronized AccessControlModule getInstance(AccessControlConfiguration accessControlConfiguration) {
    if (instance == null) {
      instance = new AccessControlModule(accessControlConfiguration);
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(new AbstractModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return accessControlConfiguration.getMongoConfig();
      }
    });
    install(AccessControlPersistenceModule.getInstance());
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();
    install(new ValidationModule(validatorFactory));
    install(ScopeModule.getInstance());
    install(PermissionsModule.getInstance());
    install(RoleModule.getInstance());
    install(RoleAssignmentModule.getInstance());
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {}
}

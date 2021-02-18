package io.harness.accesscontrol;

import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.ACCOUNT;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.ORGANIZATION;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.PROJECT;

import io.harness.DecisionModule;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParamsFactory;
import io.harness.mongo.MongoConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
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
    install(AccessControlCoreModule.getInstance());
    install(DecisionModule.getInstance(accessControlConfiguration.getDecisionModuleConfiguration()));

    MapBinder<String, ScopeLevel> scopesByKey = MapBinder.newMapBinder(binder(), String.class, ScopeLevel.class);
    scopesByKey.addBinding(ACCOUNT.toString()).toInstance(ACCOUNT);
    scopesByKey.addBinding(ORGANIZATION.toString()).toInstance(ORGANIZATION);
    scopesByKey.addBinding(PROJECT.toString()).toInstance(PROJECT);

    bind(ScopeParamsFactory.class).to(HarnessScopeParamsFactory.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {}
}

package io.harness.accesscontrol;

import static io.harness.AuthorizationServiceHeader.ACCESS_CONTROL_SERVICE;
import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.ACCOUNT;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.ORGANIZATION;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.PROJECT;

import io.harness.AccessControlClientModule;
import io.harness.DecisionModule;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.PrincipalValidator;
import io.harness.accesscontrol.principals.user.UserValidator;
import io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupService;
import io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupServiceImpl;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParamsFactory;
import io.harness.aggregator.AggregatorModule;
import io.harness.ng.core.UserClientModule;
import io.harness.resourcegroupclient.ResourceGroupClientModule;
import io.harness.threading.ExecutorModule;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import java.util.concurrent.Executors;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import ru.vyarus.guice.validator.ValidationModule;

public class AccessControlModule extends AbstractModule {
  private static AccessControlModule instance;
  private final AccessControlConfiguration config;

  private AccessControlModule(AccessControlConfiguration config) {
    this.config = config;
  }

  public static synchronized AccessControlModule getInstance(AccessControlConfiguration config) {
    if (instance == null) {
      instance = new AccessControlModule(config);
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(AccessControlPersistenceModule.getInstance(config.getMongoConfig()));
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();
    install(new ValidationModule(validatorFactory));
    install(AccessControlCoreModule.getInstance());
    install(DecisionModule.getInstance(config.getDecisionModuleConfiguration()));
    ExecutorModule.getInstance().setExecutorService(Executors.newFixedThreadPool(5));
    install(ExecutorModule.getInstance());
    install(AggregatorModule.getInstance(
        config.getAggregatorConfiguration(), ExecutorModule.getInstance().getExecutorService()));

    install(AccessControlClientModule.getInstance(
        config.getAccessControlClientConfiguration(), ACCESS_CONTROL_SERVICE.getServiceId()));
    install(new ResourceGroupClientModule(config.getResourceGroupClientConfiguration().getResourceGroupServiceConfig(),
        config.getResourceGroupClientConfiguration().getResourceGroupServiceSecret(),
        ACCESS_CONTROL_SERVICE.getServiceId()));
    install(new UserClientModule(config.getUserClientConfiguration().getUserServiceConfig(),
        config.getUserClientConfiguration().getUserServiceSecret(), ACCESS_CONTROL_SERVICE.getServiceId()));

    MapBinder<String, ScopeLevel> scopesByKey = MapBinder.newMapBinder(binder(), String.class, ScopeLevel.class);
    scopesByKey.addBinding(ACCOUNT.toString()).toInstance(ACCOUNT);
    scopesByKey.addBinding(ORGANIZATION.toString()).toInstance(ORGANIZATION);
    scopesByKey.addBinding(PROJECT.toString()).toInstance(PROJECT);
    bind(ScopeParamsFactory.class).to(HarnessScopeParamsFactory.class);

    bind(HarnessResourceGroupService.class).to(HarnessResourceGroupServiceImpl.class);

    MapBinder<PrincipalType, PrincipalValidator> validatorByPrincipalType =
        MapBinder.newMapBinder(binder(), PrincipalType.class, PrincipalValidator.class);
    validatorByPrincipalType.addBinding(USER).to(UserValidator.class);

    registerRequiredBindings();
  }

  private void registerRequiredBindings() {}
}

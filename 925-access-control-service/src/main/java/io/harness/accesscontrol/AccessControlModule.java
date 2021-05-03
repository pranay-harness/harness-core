package io.harness.accesscontrol;

import static io.harness.AuthorizationServiceHeader.ACCESS_CONTROL_SERVICE;
import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.ACCOUNT;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.ORGANIZATION;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.PROJECT;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkConstants.DUMMY_GROUP_NAME;
import static io.harness.eventsframework.EventsFrameworkConstants.DUMMY_TOPIC_NAME;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD_MAX_PROCESSING_TIME;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD_READ_BATCH_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.FEATURE_FLAG_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.USERMEMBERSHIP;
import static io.harness.lock.DistributedLockImplementation.MONGO;

import io.harness.AccessControlClientModule;
import io.harness.DecisionModule;
import io.harness.accesscontrol.commons.events.EventConsumer;
import io.harness.accesscontrol.commons.iterators.AccessControlIteratorsConfig;
import io.harness.accesscontrol.commons.outbox.AccessControlOutboxEventHandler;
import io.harness.accesscontrol.preference.AccessControlPreferenceModule;
import io.harness.accesscontrol.preference.events.NGRBACEnabledFeatureFlagEventConsumer;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.PrincipalValidator;
import io.harness.accesscontrol.principals.user.UserValidator;
import io.harness.accesscontrol.principals.user.events.UserMembershipEventConsumer;
import io.harness.accesscontrol.principals.usergroups.HarnessUserGroupService;
import io.harness.accesscontrol.principals.usergroups.HarnessUserGroupServiceImpl;
import io.harness.accesscontrol.principals.usergroups.UserGroupValidator;
import io.harness.accesscontrol.principals.usergroups.events.UserGroupEventConsumer;
import io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupService;
import io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupServiceImpl;
import io.harness.accesscontrol.resources.resourcegroups.events.ResourceGroupEventConsumer;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParamsFactory;
import io.harness.aggregator.AggregatorModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.client.remote.AuditClientModule;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.lock.DistributedLockImplementation;
import io.harness.lock.PersistentLockModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.outbox.OutboxPollConfiguration;
import io.harness.outbox.TransactionOutboxModule;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.redis.RedisConfig;
import io.harness.resourcegroupclient.ResourceGroupClientModule;
import io.harness.serializer.morphia.OutboxEventMorphiaRegistrar;
import io.harness.usergroups.UserGroupClientModule;
import io.harness.usermembership.UserMembershipClientModule;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.time.Duration;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import ru.vyarus.guice.validator.ValidationModule;

@OwnedBy(PL)
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

  @Provides
  @Singleton
  DistributedLockImplementation distributedLockImplementation() {
    return config.getDistributedLockImplementation() == null ? MONGO : config.getDistributedLockImplementation();
  }

  @Provides
  @Named("lock")
  @Singleton
  public RedisConfig redisLockConfig() {
    return config.getRedisLockConfig();
  }

  @Provides
  @Named(ENTITY_CRUD)
  public Consumer getEntityCrudConsumer() {
    RedisConfig redisConfig = config.getEventsConfig().getRedisConfig();
    if (!config.getEventsConfig().isEnabled()) {
      return NoOpConsumer.of(DUMMY_TOPIC_NAME, DUMMY_GROUP_NAME);
    }
    return RedisConsumer.of(ENTITY_CRUD, ACCESS_CONTROL_SERVICE.getServiceId(), redisConfig,
        ENTITY_CRUD_MAX_PROCESSING_TIME, ENTITY_CRUD_READ_BATCH_SIZE);
  }

  @Provides
  @Named(FEATURE_FLAG_STREAM)
  public Consumer getFeatureFlagConsumer() {
    RedisConfig redisConfig = config.getEventsConfig().getRedisConfig();
    if (!config.getEventsConfig().isEnabled()) {
      return NoOpConsumer.of(DUMMY_TOPIC_NAME, DUMMY_GROUP_NAME);
    }
    return RedisConsumer.of(
        FEATURE_FLAG_STREAM, ACCESS_CONTROL_SERVICE.getServiceId(), redisConfig, Duration.ofMinutes(10), 3);
  }

  @Provides
  @Named(USERMEMBERSHIP)
  public Consumer getUserMembershipConsumer() {
    RedisConfig redisConfig = config.getEventsConfig().getRedisConfig();
    if (!config.getEventsConfig().isEnabled()) {
      return NoOpConsumer.of(DUMMY_TOPIC_NAME, DUMMY_GROUP_NAME);
    }
    return RedisConsumer.of(
        USERMEMBERSHIP, ACCESS_CONTROL_SERVICE.getServiceId(), redisConfig, Duration.ofMinutes(10), 3);
  }

  @Provides
  public AccessControlIteratorsConfig getIteratorsConfig() {
    return config.getIteratorsConfig();
  }

  @Provides
  @Singleton
  public OutboxPollConfiguration getOutboxPollConfiguration() {
    return config.getOutboxPollConfig();
  }

  @Override
  protected void configure() {
    install(AccessControlPersistenceModule.getInstance(config.getMongoConfig()));
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();
    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
    install(PersistentLockModule.getInstance());
    Multibinder<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends MorphiaRegistrar>>() {});
    morphiaRegistrars.addBinding().toInstance(OutboxEventMorphiaRegistrar.class);
    install(new TransactionOutboxModule());
    bind(OutboxEventHandler.class).to(AccessControlOutboxEventHandler.class);
    install(new ValidationModule(validatorFactory));
    install(AccessControlCoreModule.getInstance());
    install(DecisionModule.getInstance(config.getDecisionModuleConfiguration()));
    if (config.getAggregatorConfiguration().isEnabled()) {
      install(AggregatorModule.getInstance(config.getAggregatorConfiguration()));
    }

    install(AccessControlClientModule.getInstance(
        config.getAccessControlClientConfiguration(), ACCESS_CONTROL_SERVICE.getServiceId()));

    install(new ResourceGroupClientModule(config.getResourceGroupClientConfiguration().getResourceGroupServiceConfig(),
        config.getResourceGroupClientConfiguration().getResourceGroupServiceSecret(),
        ACCESS_CONTROL_SERVICE.getServiceId()));

    install(new UserGroupClientModule(config.getUserGroupClientConfiguration().getUserGroupServiceConfig(),
        config.getUserGroupClientConfiguration().getUserGroupServiceSecret(), ACCESS_CONTROL_SERVICE.getServiceId()));

    install(new UserMembershipClientModule(config.getUserClientConfiguration().getUserServiceConfig(),
        config.getUserClientConfiguration().getUserServiceSecret(), ACCESS_CONTROL_SERVICE.getServiceId()));

    install(new AuditClientModule(config.getAuditClientConfig(), config.getDefaultServiceSecret(),
        ACCESS_CONTROL_SERVICE.getServiceId(), config.isEnableAudit()));

    install(AccessControlPreferenceModule.getInstance(config.getAccessControlPreferenceConfiguration()));

    MapBinder<String, ScopeLevel> scopesByKey = MapBinder.newMapBinder(binder(), String.class, ScopeLevel.class);
    scopesByKey.addBinding(ACCOUNT.toString()).toInstance(ACCOUNT);
    scopesByKey.addBinding(ORGANIZATION.toString()).toInstance(ORGANIZATION);
    scopesByKey.addBinding(PROJECT.toString()).toInstance(PROJECT);
    bind(ScopeParamsFactory.class).to(HarnessScopeParamsFactory.class);

    bind(HarnessResourceGroupService.class).to(HarnessResourceGroupServiceImpl.class);
    bind(HarnessUserGroupService.class).to(HarnessUserGroupServiceImpl.class);

    MapBinder<PrincipalType, PrincipalValidator> validatorByPrincipalType =
        MapBinder.newMapBinder(binder(), PrincipalType.class, PrincipalValidator.class);
    validatorByPrincipalType.addBinding(USER).to(UserValidator.class);
    validatorByPrincipalType.addBinding(USER_GROUP).to(UserGroupValidator.class);

    Multibinder<EventConsumer> entityCrudEventConsumers =
        Multibinder.newSetBinder(binder(), EventConsumer.class, Names.named(ENTITY_CRUD));
    entityCrudEventConsumers.addBinding().to(ResourceGroupEventConsumer.class);
    entityCrudEventConsumers.addBinding().to(UserGroupEventConsumer.class);

    Multibinder<EventConsumer> featureFlagEventConsumers =
        Multibinder.newSetBinder(binder(), EventConsumer.class, Names.named(FEATURE_FLAG_STREAM));
    featureFlagEventConsumers.addBinding().to(NGRBACEnabledFeatureFlagEventConsumer.class);

    Multibinder<EventConsumer> userMembershipEventConsumers =
        Multibinder.newSetBinder(binder(), EventConsumer.class, Names.named(USERMEMBERSHIP));
    userMembershipEventConsumers.addBinding().to(UserMembershipEventConsumer.class);

    registerRequiredBindings();
  }

  private void registerRequiredBindings() {}
}

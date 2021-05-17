package io.harness.ccm;

import static io.harness.AuthorizationServiceHeader.MANAGER;
import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CONNECTOR_ENTITY;
import static io.harness.lock.DistributedLockImplementation.MONGO;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.retry.MethodExecutionHelper;
import io.harness.annotations.retry.RetryOnException;
import io.harness.annotations.retry.RetryOnExceptionInterceptor;
import io.harness.app.PrimaryVersionManagerModule;
import io.harness.ccm.eventframework.ConnectorEntityCRUDStreamListener;
import io.harness.ccm.persistence.JooqExecuteListener;
import io.harness.connector.ConnectorResourceClientModule;
import io.harness.ff.FeatureFlagModule;
import io.harness.govern.ProviderMethodInterceptor;
import io.harness.govern.ProviderModule;
import io.harness.lock.DistributedLockImplementation;
import io.harness.mongo.AbstractMongoModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.event.MessageListener;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.UserProvider;
import io.harness.queryconverter.SQLConverter;
import io.harness.queryconverter.SQLConverterImpl;
import io.harness.redis.RedisConfig;
import io.harness.serializer.CENextGenRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.morphia.PrimaryVersionManagerMorphiaRegistrar;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import io.harness.timescaledb.JooqModule;
import io.harness.timescaledb.TimeScaleDBConfig;
import io.harness.version.VersionModule;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.Map;
import java.util.Set;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.jooq.ExecuteListener;
import org.mongodb.morphia.converters.TypeConverter;
import ru.vyarus.guice.validator.ValidationModule;

@OwnedBy(CE)
public class CENextGenModule extends AbstractModule {
  private final CENextGenConfiguration configuration;

  public CENextGenModule(CENextGenConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    install(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(CENextGenRegistrars.kryoRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(CENextGenRegistrars.morphiaRegistrars)
            .add(PrimaryVersionManagerMorphiaRegistrar.class)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder().build();
      }

      @Provides
      @Singleton
      MongoConfig eventsMongoConfig() {
        return configuration.getEventsMongoConfig();
      }

      @Provides
      @Singleton
      @Named("TimeScaleDBConfig")
      TimeScaleDBConfig timeScaleDBConfig() {
        return configuration.getTimeScaleDBConfig();
      }

      @Provides
      @Singleton
      @Named("PSQLExecuteListener")
      ExecuteListener executeListener() {
        return new JooqExecuteListener();
      }
    });

    install(ExecutorModule.getInstance());
    install(new AbstractMongoModule() {
      @Override
      public UserProvider userProvider() {
        return new NoopUserProvider();
      }
    });
    install(new ConnectorResourceClientModule(
        configuration.getNgManagerClientConfig(), configuration.getNgManagerServiceSecret(), MANAGER.getServiceId()));
    install(VersionModule.getInstance());
    install(PrimaryVersionManagerModule.getInstance());
    install(new ValidationModule(getValidatorFactory()));
    install(TimeModule.getInstance());
    install(FeatureFlagModule.getInstance());
    install(new EventsFrameworkModule(configuration.getEventsFrameworkConfiguration()));
    install(JooqModule.getInstance());
    bind(HPersistence.class).to(MongoPersistence.class);
    bind(CENextGenConfiguration.class).toInstance(configuration);
    bind(SQLConverter.class).to(SQLConverterImpl.class);
    registerEventsFrameworkMessageListeners();

    bindRetryOnExceptionInterceptor();
  }

  private void bindRetryOnExceptionInterceptor() {
    bind(MethodExecutionHelper.class).asEagerSingleton();
    ProviderMethodInterceptor retryOnExceptionInterceptor =
        new ProviderMethodInterceptor(getProvider(RetryOnExceptionInterceptor.class));
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(RetryOnException.class), retryOnExceptionInterceptor);
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return ImmutableMap.<Class, String>builder().build();
  }

  @Provides
  @Singleton
  DistributedLockImplementation distributedLockImplementation() {
    return MONGO;
  }

  @Provides
  @Named("lock")
  @Singleton
  RedisConfig redisConfig() {
    return RedisConfig.builder().build();
  }

  private ValidatorFactory getValidatorFactory() {
    return Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new ReflectionParameterNameProvider())
        .buildValidatorFactory();
  }

  private void registerEventsFrameworkMessageListeners() {
    bind(MessageListener.class)
        .annotatedWith(Names.named(CONNECTOR_ENTITY + ENTITY_CRUD))
        .to(ConnectorEntityCRUDStreamListener.class);
  }
}

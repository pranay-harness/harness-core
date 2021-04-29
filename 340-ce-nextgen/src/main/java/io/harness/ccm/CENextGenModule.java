package io.harness.ccm;

import static io.harness.AuthorizationServiceHeader.MANAGER;
import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CONNECTOR_ENTITY;
import static io.harness.lock.DistributedLockImplementation.MONGO;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.eventframework.ConnectorEntityCRUDStreamListener;
import io.harness.connector.ConnectorResourceClientModule;
import io.harness.ff.FeatureFlagModule;
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
import io.harness.queue.QueueController;
import io.harness.redis.RedisConfig;
import io.harness.serializer.CENextGenRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.threading.ExecutorModule;
import io.harness.timescaledb.DSLContextService;
import io.harness.version.VersionModule;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.Map;
import java.util.Set;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.jooq.DSLContext;
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
    install(new ValidationModule(getValidatorFactory()));
    install(FeatureFlagModule.getInstance());
    install(new EventsFrameworkModule(configuration.getEventsFrameworkConfiguration()));
    bind(HPersistence.class).to(MongoPersistence.class);
    bind(CENextGenConfiguration.class).toInstance(configuration);
    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
    registerEventsFrameworkMessageListeners();
    bind(DSLContext.class)
        .toInstance(new DSLContextService(configuration.getTimeScaleDBConfig()).getDefaultDSLContext());

    install(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });
      }
    });
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

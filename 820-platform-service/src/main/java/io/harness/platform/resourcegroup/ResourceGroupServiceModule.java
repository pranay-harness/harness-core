package io.harness.platform.resourcegroup;

import static io.harness.AuthorizationServiceHeader.RESOUCE_GROUP_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.lock.DistributedLockImplementation.MONGO;

import io.harness.AccessControlClientModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.client.remote.AuditClientModule;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.govern.ProviderModule;
import io.harness.lock.DistributedLockImplementation;
import io.harness.lock.PersistentLockModule;
import io.harness.mongo.AbstractMongoModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.outbox.OutboxPollConfiguration;
import io.harness.outbox.OutboxSDKConstants;
import io.harness.outbox.TransactionOutboxModule;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.UserProvider;
import io.harness.platform.PlatformConfiguration;
import io.harness.queue.QueueController;
import io.harness.redis.RedisConfig;
import io.harness.resourcegroup.ResourceGroupModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.morphia.ResourceGroupSerializer;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import io.harness.version.VersionModule;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.Set;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.mongodb.morphia.converters.TypeConverter;
import ru.vyarus.guice.validator.ValidationModule;

@Slf4j
@OwnedBy(PL)
public class ResourceGroupServiceModule extends AbstractModule {
  private static final String RESOURCE_GROUP_CONSUMER_GROUP = "resource-group";
  PlatformConfiguration appConfig;

  public ResourceGroupServiceModule(PlatformConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  public void configure() {
    install(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(ResourceGroupSerializer.kryoRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(ResourceGroupSerializer.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder().build();
      }

      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return appConfig.getResoureGroupServiceConfig().getMongoConfig();
      }
    });
    install(new AbstractMongoModule() {
      @Override
      public UserProvider userProvider() {
        return new NoopUserProvider();
      }
    });
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
    install(ExecutorModule.getInstance());
    bind(PlatformConfiguration.class).toInstance(appConfig);
    bind(HPersistence.class).to(MongoPersistence.class);
    install(VersionModule.getInstance());
    install(new ValidationModule(getValidatorFactory()));
    install(new ResourceGroupPersistenceModule());
    install(PersistentLockModule.getInstance());
    install(TimeModule.getInstance());
    install(new ResourceGroupModule(appConfig.getResoureGroupServiceConfig()));
    install(new AuditClientModule(this.appConfig.getResoureGroupServiceConfig().getAuditClientConfig(),
        this.appConfig.getPlatformSecrets().getNgManagerServiceSecret(), RESOUCE_GROUP_SERVICE.getServiceId(),
        this.appConfig.getResoureGroupServiceConfig().isEnableAudit()));
    install(AccessControlClientModule.getInstance(
        this.appConfig.getAccessControlClientConfig(), RESOUCE_GROUP_SERVICE.getServiceId()));
    install(new TransactionOutboxModule());
  }

  @Provides
  @Singleton
  DistributedLockImplementation distributedLockImplementation() {
    return appConfig.getResoureGroupServiceConfig().getDistributedLockImplementation() == null
        ? MONGO
        : appConfig.getResoureGroupServiceConfig().getDistributedLockImplementation();
  }

  @Provides
  @Named("lock")
  @Singleton
  RedisConfig redisLockConfig() {
    return appConfig.getResoureGroupServiceConfig().getRedisConfig();
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return ImmutableMap.<Class, String>builder().build();
  }

  private ValidatorFactory getValidatorFactory() {
    return Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new ReflectionParameterNameProvider())
        .buildValidatorFactory();
  }

  @Provides
  @Singleton
  public OutboxPollConfiguration getOutboxPollConfiguration() {
    return OutboxSDKConstants.DEFAULT_OUTBOX_POLL_CONFIGURATION;
  }

  @Provides
  @Named(EventsFrameworkConstants.ENTITY_CRUD)
  Producer getProducer() {
    RedisConfig redisConfig = appConfig.getResoureGroupServiceConfig().getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      return NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME);
    }
    return RedisProducer.of(EventsFrameworkConstants.ENTITY_CRUD, redisConfig,
        EventsFrameworkConstants.ENTITY_CRUD_MAX_TOPIC_SIZE, RESOUCE_GROUP_SERVICE.getServiceId());
  }

  @Provides
  @Named(EventsFrameworkConstants.ENTITY_CRUD)
  Consumer getConsumer() {
    RedisConfig redisConfig = appConfig.getResoureGroupServiceConfig().getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      return NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME);
    }
    return RedisConsumer.of(EventsFrameworkConstants.ENTITY_CRUD, RESOURCE_GROUP_CONSUMER_GROUP, redisConfig,
        EventsFrameworkConstants.ENTITY_CRUD_MAX_PROCESSING_TIME, EventsFrameworkConstants.ENTITY_CRUD_READ_BATCH_SIZE);
  }
}

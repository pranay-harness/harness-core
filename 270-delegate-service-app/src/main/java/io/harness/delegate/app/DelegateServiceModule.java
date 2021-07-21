package io.harness.delegate.app;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.hazelcast.HazelcastModule;
import io.harness.persistence.HPersistence;
import io.harness.redis.RedisConfig;
import io.harness.waiter.AbstractWaiterModule;
import io.harness.waiter.WaiterConfiguration;

import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.security.EncryptedSettingAttributes;
import software.wings.service.intfc.security.SecretManager;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.Map;
import java.util.Set;

@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateServiceModule extends AbstractModule {
  private final DelegateServiceConfig config;

  /**
   * Delegate Service App Config.
   */
  public DelegateServiceModule(DelegateServiceConfig config) {
    this.config = config;
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return ImmutableMap.<Class, String>builder().build();
  }

  @Override
  protected void configure() {
    install(HazelcastModule.getInstance());
    bind(RedisConfig.class).annotatedWith(Names.named("atmosphere")).toInstance(config.getRedisAtmosphereConfig());
    bind(HPersistence.class).to(WingsMongoPersistence.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class);
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
    bind(EncryptedSettingAttributes.class).to(NoOpSecretManagerImpl.class);
    install(new AbstractWaiterModule() {
      @Override
      public WaiterConfiguration waiterConfiguration() {
        return WaiterConfiguration.builder().persistenceLayer(WaiterConfiguration.PersistenceLayer.MORPHIA).build();
      }
    });

    install(new DelegateServiceGrpcServerModule(config));
    install(new DelegateServiceClassicGrpcServerModule(config));
  }
}

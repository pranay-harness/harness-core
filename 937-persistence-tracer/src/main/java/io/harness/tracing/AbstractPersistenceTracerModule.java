package io.harness.tracing;

import io.harness.cache.HarnessCacheManager;
import io.harness.mongo.tracing.TracerConstants;
import io.harness.redis.RedisConfig;
import io.harness.version.VersionInfoManager;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public abstract class AbstractPersistenceTracerModule extends AbstractModule {
  @Override
  protected void configure() {
    install(PersistenceTracerModule.getInstance());
    requireBinding(HarnessCacheManager.class);
    requireBinding(VersionInfoManager.class);
  }

  @Provides
  @Singleton
  RedisConfig redisConfig() {
    return redisConfigProvider();
  }

  @Provides
  @Named(TracerConstants.SERVICE_ID)
  public String serviceId() {
    return serviceIdProvider();
  }

  protected abstract RedisConfig redisConfigProvider();

  protected abstract String serviceIdProvider();
}

package io.harness;

import io.harness.lock.DistributedLockImplementation;
import io.harness.lock.PersistentLocker;
import io.harness.lock.redis.RedisPersistentLocker;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class EventsClientApplicationModule extends AbstractModule {
  private final EventsClientApplicationConfiguration appConfig;

  public EventsClientApplicationModule(EventsClientApplicationConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Provides
  @Singleton
  PersistentLocker persistentLocker(Provider<RedisPersistentLocker> redisPersistentLockerProvider) {
    return redisPersistentLockerProvider.get();
  }

  protected void configure() {
    install(new EventsFrameworkModule(this.appConfig.getEventsFrameworkConfiguration()));
    bind(RedisConfig.class).annotatedWith(Names.named("lock")).toInstance(this.appConfig.getRedisLockConfig());
  }
}

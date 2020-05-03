package io.harness.lock;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.govern.DependencyModule;
import io.harness.govern.DependencyProviderModule;
import io.harness.lock.mongo.MongoPersistentLocker;
import io.harness.lock.redis.RedisPersistentLocker;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Set;

@Slf4j
public class PersistentLockModule extends DependencyProviderModule {
  private static volatile PersistentLockModule instance;

  public static PersistentLockModule getInstance() {
    if (instance == null) {
      instance = new PersistentLockModule();
    }
    return instance;
  }

  @Provides
  @Singleton
  PersistentLocker persistentLocker(DistributedLockImplementation distributedLockImplementation,
      Provider<RedisPersistentLocker> redisPersistentLockerProvider,
      Provider<MongoPersistentLocker> mongoPersistentLockerProvider) {
    switch (distributedLockImplementation) {
      case REDIS:
        logger.info("Initialize Redis Locker");
        return redisPersistentLockerProvider.get();
      case MONGO:
        logger.info("Initialize Mongo Locker");
        return mongoPersistentLockerProvider.get();
      default:
        throw new UnsupportedOperationException();
    }
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return Collections.emptySet();
  }
}

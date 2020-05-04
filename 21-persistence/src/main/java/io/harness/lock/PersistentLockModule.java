package io.harness.lock;

import static io.harness.lock.mongo.MongoPersistentLocker.LOCKS_COLLECTION;
import static java.util.Arrays.asList;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.mongodb.MongoClient;
import io.harness.govern.DependencyModule;
import io.harness.govern.DependencyProviderModule;
import io.harness.govern.ServersModule;
import io.harness.lock.mongo.MongoPersistentLocker;
import io.harness.lock.noop.PersistentNoopLocker;
import io.harness.lock.redis.RedisPersistentLocker;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
public class PersistentLockModule extends DependencyProviderModule implements ServersModule {
  private static volatile PersistentLockModule instance;
  private DistributedLockSvc distributedLockSvc;

  public static PersistentLockModule getInstance() {
    if (instance == null) {
      instance = new PersistentLockModule();
    }
    return instance;
  }

  @Provides
  @Singleton
  DistributedLockSvc distributedLockSvc(
      @Named("locksMongoClient") MongoClient mongoClient, @Named("locksDatabase") String locksDB) {
    DistributedLockSvcOptions distributedLockSvcOptions =
        new DistributedLockSvcOptions(mongoClient, locksDB, LOCKS_COLLECTION);
    distributedLockSvcOptions.setEnableHistory(false);

    distributedLockSvc = new DistributedLockSvcFactory(distributedLockSvcOptions).getLockSvc();
    if (distributedLockSvc != null && !distributedLockSvc.isRunning()) {
      distributedLockSvc.startup();
    }
    return distributedLockSvc;
  }

  @Provides
  @Singleton
  MongoPersistentLocker mongoPersistentLocker(
      HPersistence persistence, DistributedLockSvc distributedLockSvc, TimeLimiter timeLimiter) {
    return new MongoPersistentLocker(persistence, distributedLockSvc, timeLimiter);
  }

  @Provides
  @Singleton
  PersistentLocker persistentLocker(DistributedLockImplementation distributedLockImplementation,
      Provider<RedisPersistentLocker> redisPersistentLockerProvider,
      Provider<MongoPersistentLocker> mongoPersistentLockerProvider) {
    switch (distributedLockImplementation) {
      case NOOP:
        logger.info("Initialize Noop Locker");
        return new PersistentNoopLocker();
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

  @Override
  public List<Closeable> servers(Injector injector) {
    return asList(new Closeable() {
      @Override
      public void close() throws IOException {
        if (distributedLockSvc != null && distributedLockSvc.isRunning()) {
          distributedLockSvc.shutdown();
        }
      }
    });
  }
}

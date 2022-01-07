/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.lock;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.lock.mongo.MongoPersistentLocker.LOCKS_COLLECTION;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.lock.mongo.MongoPersistentLocker;
import io.harness.lock.noop.PersistentNoopLocker;
import io.harness.lock.redis.RedisPersistentLocker;
import io.harness.persistence.HPersistence;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.MongoClient;
import java.io.Closeable;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class PersistentLockModule extends ProviderModule implements ServersModule {
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
        log.info("Initialize Noop Locker");
        return new PersistentNoopLocker();
      case REDIS:
        log.info("Initialize Redis Locker");
        return redisPersistentLockerProvider.get();
      case MONGO:
        log.info("Initialize Mongo Locker");
        return mongoPersistentLockerProvider.get();
      default:
        throw new UnsupportedOperationException();
    }
  }

  @Override
  public List<Closeable> servers(Injector injector) {
    return asList(() -> {
      if (distributedLockSvc != null && distributedLockSvc.isRunning()) {
        distributedLockSvc.shutdown();
      }
    });
  }
}

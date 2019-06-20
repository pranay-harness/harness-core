package io.harness.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import io.harness.govern.ProviderModule;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;

@Slf4j
public class TestMongoModule extends ProviderModule {
  private AdvancedDatastore primaryDatastore;
  private DistributedLockSvc distributedLockSvc;

  public TestMongoModule(AdvancedDatastore primaryDatastore, DistributedLockSvc distributedLockSvc) {
    this.primaryDatastore = primaryDatastore;
    this.distributedLockSvc = distributedLockSvc;
  }

  @Provides
  @Named("primaryDatastore")
  public AdvancedDatastore primaryDatastore() {
    return primaryDatastore;
  }

  @Provides
  @Singleton
  public DistributedLockSvc distributedLockSvc() {
    return distributedLockSvc;
  }
}

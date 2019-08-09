package io.harness.module;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import io.harness.govern.DependencyModule;
import io.harness.govern.DependencyProviderModule;
import io.harness.morphia.MorphiaModule;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;

import java.util.Set;

@Slf4j
public class TestMongoModule extends DependencyProviderModule {
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

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(MorphiaModule.getInstance());
  }
}

package io.harness.module;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;

@Slf4j
public class TestMongoModule extends AbstractModule {
  private AdvancedDatastore primaryDatastore;
  private AdvancedDatastore secondaryDatastore;
  private DistributedLockSvc distributedLockSvc;

  public TestMongoModule(
      AdvancedDatastore primaryDatastore, AdvancedDatastore secondaryDatastore, DistributedLockSvc distributedLockSvc) {
    this.primaryDatastore = primaryDatastore;
    this.secondaryDatastore = secondaryDatastore;
    this.distributedLockSvc = distributedLockSvc;
  }

  @Override
  protected void configure() {
    bind(AdvancedDatastore.class).annotatedWith(Names.named("primaryDatastore")).toInstance(primaryDatastore);
    bind(AdvancedDatastore.class).annotatedWith(Names.named("secondaryDatastore")).toInstance(secondaryDatastore);
    bind(DistributedLockSvc.class).toInstance(distributedLockSvc);
  }
}

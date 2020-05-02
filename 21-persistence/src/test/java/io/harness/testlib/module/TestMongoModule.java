package io.harness.testlib.module;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.MongoClient;
import io.harness.govern.DependencyModule;
import io.harness.govern.DependencyProviderModule;
import io.harness.morphia.MorphiaModule;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;

import java.util.Set;

@Slf4j
public class TestMongoModule extends DependencyProviderModule {
  private AdvancedDatastore primaryDatastore;
  private MongoClient locksMongoClient;

  public TestMongoModule(AdvancedDatastore primaryDatastore, MongoClient locksMongoClient) {
    this.primaryDatastore = primaryDatastore;
    this.locksMongoClient = locksMongoClient;
  }

  public TestMongoModule(AdvancedDatastore primaryDatastore) {
    this.primaryDatastore = primaryDatastore;
  }

  @Provides
  @Named("primaryDatastore")
  @Singleton
  public AdvancedDatastore primaryDatastore() {
    return primaryDatastore;
  }

  @Provides
  @Named("locksMongoClient")
  @Singleton
  public MongoClient getLocksMongoClient() {
    return locksMongoClient;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(MorphiaModule.getInstance());
  }
}

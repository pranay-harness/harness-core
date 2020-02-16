package io.harness.module;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
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
  private String locksDB;

  public TestMongoModule(AdvancedDatastore primaryDatastore, MongoClient locksMongoClient, String locksDB) {
    this.primaryDatastore = primaryDatastore;
    this.locksMongoClient = locksMongoClient;
    this.locksDB = locksDB;
  }

  public TestMongoModule(AdvancedDatastore primaryDatastore) {
    this.primaryDatastore = primaryDatastore;
  }

  @Provides
  @Named("primaryDatastore")
  public AdvancedDatastore primaryDatastore() {
    return primaryDatastore;
  }

  @Provides
  @Named("locksMongoClient")
  public MongoClient getLocksMongoClient() {
    return locksMongoClient;
  }

  @Provides
  @Named("locksDatabase")
  public String getLocksDB() {
    return locksDB;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(MorphiaModule.getInstance());
  }
}

package io.harness.testlib.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.govern.ProviderModule;

public class TestMongoDatabaseName extends ProviderModule {
  private String databaseName;
  public TestMongoDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  @Provides
  @Named("locksDatabase")
  @Singleton
  public String getLocksDB() {
    return databaseName;
  }
}

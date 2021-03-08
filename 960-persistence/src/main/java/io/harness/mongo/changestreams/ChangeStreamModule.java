package io.harness.mongo.changestreams;

import io.harness.mongo.MongoConfig;
import io.harness.persistence.HPersistence;

import com.google.inject.AbstractModule;

public class ChangeStreamModule extends AbstractModule {
  private static volatile ChangeStreamModule instance;

  public static ChangeStreamModule getInstance() {
    if (instance == null) {
      instance = new ChangeStreamModule();
    }
    return instance;
  }

  @Override
  public void configure() {
    requireBinding(HPersistence.class);
    requireBinding(MongoConfig.class);
    requireBinding(ChangeTracker.class);
  }
}

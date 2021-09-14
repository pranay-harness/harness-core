/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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

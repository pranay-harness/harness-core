/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

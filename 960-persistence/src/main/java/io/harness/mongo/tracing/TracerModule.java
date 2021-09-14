/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo.tracing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.PIPELINE)
public class TracerModule extends AbstractModule {
  private static TracerModule instance;

  public static TracerModule getInstance() {
    if (instance == null) {
      instance = new TracerModule();
    }
    return instance;
  }

  @Override
  public void configure() {}
}

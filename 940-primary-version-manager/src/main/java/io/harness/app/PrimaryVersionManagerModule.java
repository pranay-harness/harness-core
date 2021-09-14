/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.app;

import io.harness.controller.PrimaryVersionController;
import io.harness.queue.QueueController;
import io.harness.version.VersionModule;

import com.google.inject.AbstractModule;

public class PrimaryVersionManagerModule extends AbstractModule {
  private static volatile PrimaryVersionManagerModule instance;

  private PrimaryVersionManagerModule() {}

  public static PrimaryVersionManagerModule getInstance() {
    if (instance == null) {
      instance = new PrimaryVersionManagerModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(VersionModule.getInstance());
    bind(QueueController.class).to(PrimaryVersionController.class);
  }
}
